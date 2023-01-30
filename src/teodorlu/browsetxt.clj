(ns teodorlu.browsetxt)

(def start-loc :smalgangen)

(def goto {:smalgangen #{:g17 :ibv}
           :g17 #{:smalgangen}
           :ibv #{:smalgangen}})

(defn -main [& args]
  (prn start-loc)
  (prn goto))
