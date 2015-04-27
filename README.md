Just an experiment for UI code. 


Taken form reagent template 
(https://github.com/reagent-project/reagent-template):

### Development mode

To run the development server, run

```
lein figwheel
```
Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

#### Optional development tools

Start the browser REPL:

```
$ lein repl
```
The Jetty server can be started by running:

```clojure
(start-server)
```
and stopped by running:
```clojure
(stop-server)
```

The browser REPL can be started by calling the following command:

```clojure
(browser-repl)
```

### Building for release

```
lein cljsbuild clean
lein ring uberjar
```

