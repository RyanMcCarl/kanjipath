(ns krad.abc
  (:require [datascript.core :as d]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.set :as clj-set]
            [krad.dsdb :as dsdb]))

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
                (apply mapv list ,,,) ; transpose
                (mapv #(remove (partial = "_") %) ,,,) ; remove _
                (mapv (fn [group v] 
                        (mapv (fn [value idx] 
                                {:grapheme/name value
                                 :grapheme/origins [] ; filled in later
                                 :grapheme/abc-group group ; e.g., "TO" or "Z"
                                 :grapheme/abc-number (inc idx)
                                 })
                              v
                              (range (count v))))
                      groups
                      ,,,)
                ; above: vec of vec of str to vec of vec of maps w/ str as val
                ))

(comment
  (->> table
       flatten
       shuffle
       (group-by :grapheme/abc-group)
       (map (fn [[group-name graphemes]]
              [group-name (sort-by :grapheme/abc-number graphemes)]))
       (sort-by #(-> % first group-to-idx))
       (mapv second)
       (map vec)
       (= table)
       )
  ;above: Execution time upper quantile : 786.375006 µs (97.5%)

  ; alternative
  (->> table
       flatten
       shuffle
       (sort-by (comp group-to-idx :grapheme/abc-group))
       (partition-by :grapheme/abc-group)
       (mapv #(into [] (sort-by :grapheme/abc-number %)))
       (= table)
       )
  ;above: Execution time upper quantile : 1.625902 ms (97.5%)
  ; or, ~2x slower.

  )


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

(d/transact! dsdb/conn (flatten table-origin))

;; for printing table Clojure-side, to files
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


