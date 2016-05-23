(ns krad.abc
  (:require [datascript.core :as d]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.set :as clj-set]))

(def abc-md "data/abc.md")
(def abc (->> abc-md
              io/resource
              slurp
              string/split-lines
              (map #(string/split % #"\t"))))

(def groups (first abc))
(def group-to-idx (apply hash-map (interleave groups
                                              (range (count groups)))))
(def table (->> abc
                rest
                (apply mapv list) ; transpose
                (mapv #(remove (partial = "_") %)) ; remove _
                (mapv (fn [v] (mapv #(hash-map :grapheme/name %
                                               :grapheme/origins []) v)))
                ; above: vec of vec of str to vec of vec of maps w/ str as val
                ))


(defn inclusive-range
  ([l] (inclusive-range l l))
  ([l r] (range l (inc r))))

(defn origin-entry [s]
  (let [group (re-find #"[A-Za-z]+" s)
        numbers (->> s
                     (re-find #"[0-9-]+")
                     (#(string/split % #"-"))
                     (map #(Integer/parseInt %))
                     (apply inclusive-range))]
    (map #(vector group (dec %)) ; dec to go from 1-index to 0-index
         numbers)))

(def abc-origins-md "data/abc-origins.md")
(def origins (->> abc-origins-md
                  io/resource
                  slurp
                  string/split-lines
                  (map #(string/split % #" +"))
                  (map (fn [[k & v]] [k (mapcat origin-entry v)]))
                  (into {})))

(def origin-to-keyword {"R" :radical
                        "J" :jouyou
                        "K" :other-kanji
                        "N" :invented})
(def origin-kw-to-char (clj-set/map-invert origin-to-keyword))

(def table-origin
  (reduce (fn [table [origin group-idx-pairs]]
            (reduce (fn [table [group idx]]
                      (update-in table
                                 [(group-to-idx group) idx :grapheme/origins]
                                 conj ,,, (get origin-to-keyword origin)))
                    table
                    group-idx-pairs))
          table
          origins))

(defn print-grapheme [{name :grapheme/name origins :grapheme/origins
                       :as grapheme}]
  (str name "/" (string/join (map origin-kw-to-char origins))))
(defn print-table [table]
  (string/join "\n"
               (map (fn [group entries]
                      (string/join "\t"
                                   (into
                                     [(or group "Group")]
                                     (if entries
                                       (map print-grapheme entries)
                                       (range 1 27)))))
                    (into [nil] groups)
                    (into [nil] table))))

(comment
  (do
    (spit "resources/data/prog-abc.tsv" (print-table table-origin))

    (->> table
         flatten
         (map :grapheme/name)
         (filter #(re-find #"[⿰⿱⿲⿳⿴⿵⿶⿷⿸⿹⿺⿻]" %))
         count)
    ; 13 out of 484 use IDCs.
    ))


