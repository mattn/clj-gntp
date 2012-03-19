(ns clj-gntp.core
  (:gen-class)
  (:import
    [java.net Socket]
    [java.io PrintWriter InputStreamReader BufferedReader]))

(def socket (atom nil))
(def in (atom nil))
(def out (atom nil))

(defn start-client [host port]
    (do
      (reset! socket (Socket. host port))
      (reset! out (PrintWriter. (. @socket getOutputStream)))
      (reset! in (BufferedReader. (InputStreamReader. (. @socket getInputStream))))))

(defn stop-client []
  (do (. @in close)
      (. @out close)
      (. @socket close)))

(defn send-line [m] (do (. @out print (str m "\r\n")) (. @out flush)))
(defn recv-line [] (. @in readLine))
(defn recv-all [] (apply str (line-seq @in)))

(defn register
  "Register notification."
  [server port appname notifications password icon]
  (do
    (start-client server port)
    (send-line "GNTP/1.0 REGISTER NONE ")
    (send-line (str "Application-Name: " appname))
    (if icon
      (send-line (str "Application-Icon: " icon)))
    (send-line (str "Notifications-Count: " (count notifications)))
    (send-line "")
    (doseq [notification notifications]
      (do
        (send-line (str "Notification-Name: " (notification :name)))
        (send-line (str "Notification-Display-Name: " "foo"))
        (send-line (str "Notification-Enabled: " "True"))
        (send-line "")))
    (send-line "")
    (recv-line)
    (stop-client)))

(defn notify
  "Send notify"
  [server port appname notify title message password url icon]
  (do
    (start-client server port)
    (send-line "GNTP/1.0 NOTIFY NONE ")
    (send-line (str "Application-Name: " appname))
    (send-line (str "Notification-Name: " notify))
    (send-line (str "Notification-Title: " title))
    (send-line (str "Notification-Text: " message))
    (if url
      (send-line (str "Notification-Callback-Target: " url)))
    (if icon
      (send-line (str "Notification-Icon: " icon)))
    (send-line "")
    (recv-line) ; TODO: error check
    (stop-client)))

(defn growl
  "growl it"
  [title message & extra]
  (do
    (register "localhost" 23053 "clj-gntp" [{:name "clj-gntp-notify"}] nil nil)
    (notify "localhost" 23053 "clj-gntp" "clj-gntp-notify" title message nil (first extra) (second extra))))

(defn -main [& args]
  (if (= (count args) 2)
    (growl (nth args 0) (nth args 1))
    (println "Usage: clj-gntp [title] [message]")))
