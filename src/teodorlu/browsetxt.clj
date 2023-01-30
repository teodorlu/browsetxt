(ns teodorlu.browsetxt
  (:require
   [babashka.process :as process]
   [babashka.cli :as cli]
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

(defn bat-markdown
  "bat-markdown ~ batman"
  [s]
  (process/shell {:in s} "bat" "--paging" "always" "--file-name" "browsetxt.md"))

(defn edn-parse-orelse [s orelse]
  (try (edn/read-string s)
       (catch Exception _ orelse)))

(defn fzf-edn [choices default]
  (let [next (fzf (map pr-str choices))]
    (edn-parse-orelse next default)))

(defn walk-show-loop-with-exit
  [start show next-loc quit?]
  (loop [loc start]
    ;; (prn [:on loc])
    (show loc)
    (when-let [next-loc (fzf-edn (next-loc loc) nil)]
      (when-not (quit? next-loc)
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
         (map #(link/resolve2 url (get-in % [:c 2 0])))
         (remove nil?))))

(comment
  (let [link {:t "Link", :c [["" [] []] [{:t "Str", :c "Aphorisms"}] ["./aphorisms/" ""]]}]
    (get-in link [:c 2 0]))

  (def !teodor-data (atom  (pandoc-url->data "https://play.teod.eu")))

  (get (:blocks @!teodor-data) 6)

  )

(defn url-walk [startpage opts]
  (let [pager (if (:bat-markdown opts) bat-markdown less)
        show (fn [loc]
               ;; (prn [:showing loc])
               (-> loc :url pandoc-url->md pager))
        next-loc (fn [loc]
                   (concat [:quit loc]
                           (for [target (url->links (:url loc))]
                             {:url target})))]
    (walk-show-loop-with-exit {:url startpage}
                              show
                              next-loc
                              (fn quit? [loc] (= :quit loc)))))

(defn -main [url & args]
  (url-walk url (cli/parse-opts args)))
