(ns teodorlu.browsetxt.link
  (:require
   [clojure.string :as str]))

(defn dir [link]
  (str/replace link #"/[^/]+$" "/"))

(defn ensure-single-trailing-slash [link]
  (str/replace link #"[/]*$" "/"))

(defn ensure-zero-trailing-slashes [link]
  (str/replace link #"[/]*$" ""))

(defn resolve [root link]
  (cond
    ;; absolute links don't need resolving
    (str/starts-with? link "http://") link
    (str/starts-with? link "https://") link

    ;; .. and ./.. links up
    (= link "..") (dir (ensure-zero-trailing-slashes root))
    (= link "./..") (dir (ensure-zero-trailing-slashes root))

    ;; in a relative link, replace ./ with the root dir
    (str/starts-with? link "./") (str/replace link #"^\./" (ensure-single-trailing-slash root))

    ;; we don't support other links (yet)!
    ;; (consider using a library for this)
    :else nil
    ))

(comment
  (for [ex '[(resolve "https://teod.eu" "./aphorisms/")
             (resolve "https://teod.eu" "https://teod.eu/aphorisms/")
             (resolve "https://teod.eu/aphorisms/" "..")
             (resolve "https://teod.eu" "https://play.sindre.me")
             (resolve "https://teod.eu/aphorisms/" "./..")
             ]]
    [ex (eval ex)])

  )
