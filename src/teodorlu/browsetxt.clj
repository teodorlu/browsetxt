(ns teodorlu.browsetxt
  (:require
   [babashka.process :as process]
   [clojure.edn :as edn]
   [clojure.string :as str]))

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
  [start show next-loc exit?]
  (loop [loc start]
    (when-let [next-loc (fzf-edn (next-loc loc) nil)]
      (when-not (exit? next-loc)
        (show loc)
        (recur next-loc)))))

(defn -main [& args]
  (walk-show-loop-with-exit :smalgangen
                            (comp less pr-str)
                            (fn next-loc [loc]
                              (concat [:exit]
                                      (get
                                       {:smalgangen #{:g17 :ibv}
                                        :g17 #{:smalgangen}
                                        :ibv #{:smalgangen}}
                                       loc
                                       #{})))
                            (fn exit? [loc] (= :exit loc)))

  (comment
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
  ;; (prn (fzf-edn [:norway :sweden ["lollololo"]] ::no-match))
  )
