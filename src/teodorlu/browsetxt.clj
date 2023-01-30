(ns teodorlu.browsetxt
  (:require
   [babashka.process :as process]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [cheshire.core :as json]
   [teodorlu.browsetxt.link :as link]
   [clojure.walk :as walk]))

(defn fzf
  "Choose a thing with fzf

  (fzf [\"apples\" \"pears\" \"pizza\")
  ;; => \"apples\"     ; depending on the user's choice!

  returns nil on failure."
  [choices]
  (let [fzf-result (process/shell {:out :string
                                   :in (str/join "\n" choices)}
                                  "fzf")]
    (when (= 0 (:exit fzf-result))
      (str/trim (:out fzf-result)))))

(defn less [s]
  (process/shell {:in s} "less"))

(defn edn-parse-orelse [s orelse]
  (try (edn/read-string s)
       (catch Exception _ orelse)))

(defn fzf-edn [choices default]
  (let [next (fzf (map pr-str choices))]
    (edn-parse-orelse next default)))

(defn walk-loop [start goto]
  (loop [start start]
    (when-let [next-loc (fzf-edn (goto start) nil)]
      (recur next-loc))))

(defn walk-show-loop
  [start show next-loc]
  (loop [loc start]
    (when-let [next-loc (fzf-edn (next-loc loc) nil)]
      (show loc)
      (recur next-loc))))

(defn walk-show-loop-with-exit
  [start show next-loc quit?]
  (loop [loc start]
    (when-let [next-loc (fzf-edn (next-loc loc) nil)]
      (when-not (quit? next-loc)
        (show loc)
        (recur next-loc)))))

(defn pandoc-url->md [url]
  (:out (process/shell {:out :string} "pandoc" "-t" "markdown" url)))

(defn pandoc-url->data [url]
  (let [process-result (process/shell {:out :string} "pandoc" "-t" "json" url)]
    (when (= 0 (:exit process-result))
      (json/parse-string (:out process-result) keyword))))

(defn url->links
  "Extract a sequence of liks from an url"
  [url]
  (let [link-nodes (atom [])]
    (walk/prewalk (fn [x] (when (= "Link" (:t x))
                            (swap! link-nodes conj x))
                    x)
                  (-> url pandoc-url->data :blocks))
    (->> @link-nodes
         (map #(link/resolve url (get-in % [:c 2 0])))
         (remove nil?))))

(comment
 (let [link {:t "Link", :c [["" [] []] [{:t "Str", :c "Aphorisms"}] ["./aphorisms/" ""]]}]
   (get-in link [:c 2 0]))

 (url->links "https://play.teod.eu"))

(defn url-walk [url]
  (let [show (comp less pandoc-url->md :url)   ; Is it a bad sign that I'm refactoring towards point-free Clojure?
        next-loc (fn [loc]
                   (concat [:quit]
                           (for [target (url->links (:url loc))]
                             {:url target})))]
    (walk-show-loop-with-exit {:url url}
                              show
                              next-loc
                              (fn quit? [loc] (= :quit loc)))))

(defn -main [& args]
  (url-walk "https://play.teod.eu")

  (comment
    (walk-show-loop-with-exit :smalgangen
                              (comp less pr-str)
                              (fn next-loc [loc]
                                (concat [:quit]
                                        (get
                                         {:smalgangen #{:g17 :ibv}
                                          :g17 #{:smalgangen}
                                          :ibv #{:smalgangen}}
                                         loc
                                         #{})))
                              (fn quit? [loc] (= :quit loc)))

    (walk-show-loop :smalgangen
                    (comp less pr-str)
                    {:smalgangen #{:g17 :ibv}
                     :g17 #{:smalgangen}
                     :ibv #{:smalgangen}})
    (do
      (less (str/join "\n" (range 100)))
      nil)

    (walk-loop :smalgangen
               {:smalgangen #{:g17 :ibv}
                :g17 #{:smalgangen}
                :ibv #{:smalgangen}}))
  )
