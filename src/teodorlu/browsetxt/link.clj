(ns teodorlu.browsetxt.link
  (:require
   [clojure.string :as str]))

(defn dir [link]
  (str/replace link #"/[^/]+$" "/"))

(defn ensure-single-trailing-slash [link]
  (str/replace link #"[/]*$" "/"))

(defn ensure-zero-trailing-slashes [link]
  (str/replace link #"[/]*$" ""))

(defn protocol [link]
  (let [[_ protocol host path page-stuff] (re-matches #"([a-z]+://)([^/]+)([^#]+)(.*)" link)]
    protocol))

(defn host [link]
  (let [[_ protocol host path page-stuff] (re-matches #"([a-z]+://)([^/]+)([^#]+)(.*)" link)]
    host))

(comment
  (protocol "https://play.teod.eu/aphorisms/index.html")
  ;; => "https://"

  (host "https://play.teod.eu/aphorisms/index.html")
  ;; => "play.teod.eu"
  )

(defn path-remove-dot-segments
  "Implementation of the RFC 3986 remove_dot_segments algorithm

  See: https://www.rfc-editor.org/rfc/rfc3986#section-5.2.4"
  [path]
  (let [[_ protocol host _ _] (re-matches #"([a-z]+://)([^/]+)([^#]+)(.*)" path)]
    (if (or protocol host)
      nil ; someone is trying to remove dot segments from something that is not a path.
          ; No good!
      (loop [in-buffer path
             out-buffer ""]
        (if (str/blank? in-buffer)
          out-buffer ; return
          ;; otherwise, ...
          (cond
            ;; 2A
            (str/starts-with? in-buffer "./")
            (recur (subs in-buffer 2)
                   out-buffer)
            (str/starts-with? in-buffer "../")
            (recur (subs in-buffer 3)
                   out-buffer)

            ;; 2B
            (str/starts-with? in-buffer "/./")
            (recur (subs in-buffer 3)
                   out-buffer)
            ;; I don't understand this part:
            ;;
            ;; > "/.", where "." is a complete path segment

            ;; 2C

            ;; bah, this is boring. What did I expect! It's just following some
            ;; really detailed instructions. I want to DESIGN! Not play
            ;; codemonkey.
            )
          )))))

(comment
  (let [p (java.net.URI. "http://example.com/foo/bar/42?param=true")]
    (.getPath p))
  ;; => "/foo/bar/42"

  (let [p (java.net.URI. "http://example.com/foo/bar/42/../43?param=true")]
    (.getPath p))
  ;; => "/foo/bar/42/../43"

  (let [p (java.net.URI. "http://example.com/foo/bar/42/../43?param=true")]
    (.getPath (.normalize p)))
  ;; => "/foo/bar/43"



  ;; https://docs.oracle.com/javase/7/docs/api/java/net/URI.html
  ;;
  ;; ooog ... jeg blir glad i java jeg :)
  ;;

  )

(comment
  (subs "./somepath" 2)
  ;; => "somepath"
  (subs "../somepath" 3)
  ;; => "somepath"


  )

(defn resolve [root link]
  (cond
    ;; absolute links don't need resolving
    (str/starts-with? link "http://") link
    (str/starts-with? link "https://") link

    ;; .. and ./.. links up
    (#{".." "./.." "../"} link) (dir (ensure-zero-trailing-slashes root))

    ;; in a relative link, replace ./ with the root dir
    (str/starts-with? link "./") (str/replace link #"^\./" (ensure-single-trailing-slash root))

    ;; absolute links start with /
    (str/starts-with? link "/") (str (protocol root) (host root) link)

    ;; we don't support other links (yet)!
    ;;
    ;; Using a library would fix this. At the same time, I like incrementally adding link cases.
    :else nil
    ))

(comment
  (for [ex '[(resolve "https://teod.eu" "./aphorisms/")
             (resolve "https://teod.eu" "https://teod.eu/aphorisms/")
             (resolve "https://teod.eu/aphorisms/" "..")
             (resolve "https://teod.eu" "https://play.sindre.me")
             (resolve "https://teod.eu/aphorisms/" "./..")
             ]]
    [ex (eval ex)]))

(defn resolve2
  "Cheat (use java)"
  [root link]
  (str (.normalize (.resolve (.normalize (java.net.URI. (str root "/")))
                             link))))

(comment

  (for [ex '[(resolve2 "https://teod.eu" "./aphorisms/")
             (resolve2 "https://teod.eu" "https://teod.eu/aphorisms/")
             (resolve2 "https://teod.eu/aphorisms/" "..")
             (resolve2 "https://teod.eu" "https://play.sindre.me")
             (resolve2 "https://teod.eu/aphorisms/" "./..")
             ]]
    [ex (eval ex)])
  ;; => ([(resolve2 "https://teod.eu" "./aphorisms/") "https://teod.eu/aphorisms/"]
  ;;     [(resolve2 "https://teod.eu" "https://teod.eu/aphorisms/") "https://teod.eu/aphorisms/"]
  ;;     [(resolve2 "https://teod.eu/aphorisms/" "..") "https://teod.eu/"]
  ;;     [(resolve2 "https://teod.eu" "https://play.sindre.me") "https://play.sindre.me"]
  ;;     [(resolve2 "https://teod.eu/aphorisms/" "./..") "https://teod.eu/"])

  )
