(ns uxbox.streams
  (:require [bacon]
            [cats.protocols :as p]
            [cats.context :as ctx])
  (:refer-clojure :exclude [true?
                            map
                            filter
                            reduce
                            merge
                            repeat
                            repeatedly
                            zip
                            dedupe
                            drop
                            take
                            take-while
                            not
                            and
                            or
                            next
                            concat
                            partition]))

;; coercions

(defn to-property
  ([obs]
   (.toProperty obs))
  ([obs inital-value]
   (.toProperty obs initial-value)))

(defn to-event-stream
  [p]
  (.toEventStream p))

(defn to-promise
  ([obs]
   (.toPromise obs))
  ([obs ctr]
   (.toPromise obs ctr)))

(defn first-to-promise
  ([obs]
   (.firstToPromise obs))
  ([obs ctr]
   (.firstToPromise obs ctr)))

;; subscription

(defn on-value
  [obs f]
  (.onValue obs f))

(defn on-error
  [obs f]
  (.onError obs f))

(defn on-end
  [obs f]
  (.onEnd obs f))

(defn subscribe
  [obs sf]
  (.subscribe obs sf))

;; property

(defn property?
  [p]
  (instance? js/Bacon.Property p))

(defn constant
  [v]
  (js/Bacon.constant v))

(defn sample
  [milis p]
  (.sample p milis))

(defn sampled-by
  ([p obs]
   (.sampledBy p obs))
  ([p obs cf]
   (.sampledBy p obs cf)))

(defn changes
  [p]
  {:pre [(property? p)]}
  (.changes p))

(defn and
  ([p1 p2]
   {:pre [(property? p1)
          (property? p2)]}
   (.and p1 p2))
  ([p1 p2 & ps]
   (cljs.core/reduce and
                     (and p1 p2)
                     ps)))

(defn or
  ([p1 p2]
   {:pre [(property? p1)
          (property? p2)]}
   (.or p1 p2))
  ([p1 p2 & ps]
   (cljs.core/reduce or
                     (or p1 p2)
                     ps)))

(def property-context
  (reify
    p/Context
    (-get-level [_] ctx/+level-default+)

    p/Functor
    (-fmap [_ f obs]
      (.map obs f))

    p/Applicative
    (-pure [_ v]
      (constant v))

    (-fapply [_ pf pv]
      (to-property (js/Bacon.zipWith #(%1 %2) pf pv)))

    p/Monad
    (-mreturn [_ v]
      (constant v))

    (-mbind [_ p f]
      (to-property (.flatMap p f)))))

(extend-type js/Bacon.Property
  p/Contextual
  (-get-context [_] property-context))

;; event stream

(defn event-stream?
  [s]
  (instance? js/Bacon.EventStream s))

(defn once
  [v]
  (js/Bacon.once v))

(defn never
  []
  (js/Bacon.never))

(defn repeat
  [rf]
  (js/Bacon.repeat rf))

(defn interval
  ([ms]
   (js/Bacon.interval ms))
  ([ms v]
   (js/Bacon.interval ms v)))

(defn later
  [ms v]
   (js/Bacon.later ms v))

(defn sequentially
  [ms coll]
  (js/Bacon.sequentially ms (into-array coll)))

(defn repeatedly
  [ms coll]
  (js/Bacon.repeatedly ms (into-array coll)))

(defn from-coll
  [coll]
  (js/Bacon.fromArray (into-array coll)))

(defn from-callback
  [cb]
  (js/Bacon.fromCallback cb))

(defn from-poll
  [interval pf]
  (js/Bacon.fromPoll interval pf))

(defn from-binder
  [bf]
  (js/Bacon.fromBinder bf))

(defn from-event
  ([target ev]
   (js/Bacon.fromEvent target ev))
  ([target ev tf]
   (js/Bacon.fromEvent target ev tf)))

(defn from-atom [atm key]
  (from-binder (fn [sink]
                 (add-watch atm key (fn [_ _ _ new-value] (sink new-value)))
                 #(remove-watch atm key))))

(defn initial
  [v]
  (js/Bacon.Initial. v))

(defn initial?
  [i]
  (instance? js/Bacon.Initial i))

(extend-type js/Bacon.Initial
  IDeref
  (-deref [ev]
    (.-valueInternal ev)))

(defn next
  [v]
  (js/Bacon.Next. v))

(defn next?
  [v]
  (instance? js/Bacon.Next v))

(extend-type js/Bacon.Next
  IDeref
  (-deref [ev]
    (.value ev)))

(defn error
  [e]
  (js/Bacon.Error. e))

(extend-type js/Bacon.Error
  IDeref
  (-deref [err]
    (.-error err)))

(defn error?
  [e]
  (instance? js/Bacon.Error e))

(defn end
  []
  (js/Bacon.End.))

(defn end?
  [e]
  (instance? js/Bacon.End e))

(defn has-value?
  [ev]
  (.hasValue ev))

(def more js/Bacon.more)
(def no-more js/Bacon.noMore)

(defn concat
  ([one other]
   (.concat one other))
  ([one other & others]
   (cljs.core/reduce concat
                     (.concat one other)
                     others)))

(defn merge
  ([one other]
   (.merge one other))
  ([one other & others]
   (cljs.core/reduce merge
                     (.merge one other)
                     others)))

(defn hold-when
  [stream valve]
  (.holdWhen stream valve))

(defn take-while
  [stream p]
  (.takeWhile stream p))

(defn take-until
  [stream p]
  (.takeUntil stream p))

(defn skip-while
  {:pre [(or (property? p)
             (fn? p))]}
  [stream p]
  (.skipWhile stream p))

(defn skip-until
  {:pre [(event-stream? s)]}
  [stream s]
  (.skipUntil stream s))

(def event-stream-context
  (reify
    p/Context
    (-get-level [_] ctx/+level-default+)

    p/Functor
    (-fmap [_ f obs]
      (.map obs f))

    p/Applicative
    (-pure [_ v]
      (once v))

    (-fapply [_ pf pv]
      (js/Bacon.zipWith #(%1 %2) pf pv))

    p/Monad
    (-mreturn [_ v]
      (once v))

    (-mbind [_ mv f]
      (.flatMap mv f))))

(extend-type js/Bacon.EventStream
  p/Contextual
  (-get-context [_] event-stream-context))

;; bus

(defn bus?
  [b]
  (instance? js/Bacon.Bus b))

(defn bus
  []
  (js/Bacon.Bus.))

(defn push!
  [b v]
  (.push b v))

(defn error!
  [b e]
  (.error b e))

(defn plug!
  [b obs]
  (.plug b obs))

(defn end!
  [b]
  (.end b))

(def bus-context
  (reify
    p/Context
    (-get-level [_] ctx/+level-default+)

    p/Functor
    (-fmap [_ f b]
      (let [nb (bus)]
        (on-value b #(push! nb (f %)))
        (on-error b #(error! nb %))
        (on-end b #(end! nb))
        nb))))

(extend-type js/Bacon.Bus
  p/Contextual
  (-get-context [_] bus-context))

;; observable

(defn observable?
  [o]
  (or (property? o)
      (event-stream? o)
      (bus? o)))


;; core

(defn flat-map
  [obs f]
  (.flatMap obs f))

(defn flat-map-latest
  [obs f]
  (.flatMapLatest obs f))

(defn flat-map-first
  [obs f]
  (.flatMapFirst obs f))

(defn reduce
  [rf seed obs]
  (.reduce obs seed rf))

(defn scan
  [rf seed obs]
  (.scan obs seed rf))

(defn take
  [n obs]
  (.take obs n))

(defn map
  [f obs]
  (.map obs f))

(declare property?)
(defn filter
  [pred obs]
  (let [pred (if (property? pred)
               pred
               #(pred %))]
    (.filter obs pred)))

(defn skip-duplicates
  ([obs]
   (.skipDuplicates obs =))
  ([obs cf]
   (.skipDuplicates obs cf)))

(defn not
  [obs]
  (.not obs))

(defn start-with
  [value obs]
  (.startWith obs value))

;; combination

(defn combine
  [cf o1 o2]
  (.combine o1 o2 cf))

(defn zip
  [o1 o2]
  (.zip o1 o2 vector))

(defn zip-with
  [zf & os]
  (js/Bacon.zipWith zf (into-array os)))

;; interop

(defn pipe-to-atom
  ([obs]
   (let [a (atom nil)]
     (pipe-to-atom a obs)))
  ([a obs]
   (on-value obs #(reset! a %))
   a)
  ([a obs f]
   (on-value obs #(swap! a f %))
   a))

(defn- sink-step
  [sink]
  (fn
    ([r]
     (sink (end))
     r)
    ([_ input]
     (sink input)
     input)))

(defn transform
  [xform stream]
  (let [ns (js/Bacon.fromBinder (fn [sink]
                                  (let [xsink (xform (sink-step sink))
                                        step (fn [input]
                                               (let [v (xsink nil input)]
                                                 (when (reduced? v)
                                                   (xsink @v))))
                                        unsub (.onValue stream step)]
                                    (.onEnd stream #(do (xsink nil)
                                                        (sink (end))))
                                    (fn []
                                      (unsub)))))]
    ns))

;; debugging

(defn log
  ([stream]
   (.log stream)
   stream)
  ([logger stream]
   (.log (map logger stream))
   stream))

(defn pr-log
  ([stream]
   (log #(pr-str %) stream)
   stream)
  ([prefix stream]
   (log #(pr-str prefix %) stream)
   stream))

;; buffering

(defn buffer-with-count
  [stream n]
  (.bufferWithCount stream n))

;; clojury aliases

(defn partition
  [n stream]
  (buffer-with-count stream n))

(defn dedupe
  [obs]
  (skip-duplicates obs))

(defn true?
  [obs]
  (filter cljs.core/true? obs))
