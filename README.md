# krad

A [re-frame](https://github.com/Day8/re-frame) application designed to ... well, that part is up to you.

## Development Mode

### Compile css:

Compile css file once.

```
lein garden once
```

Automatically recompile css file on change.

```
lein garden auto
```

### Start figwheel

```
lein clean
lein figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

### Start the webserver
```
lein run
```

Wait a bit, then browse to [http://localhost:9090](http://localhost:9090).

### Connect a live nrepl session
```
$ lein repl :connect 9091
```

## Production Build

```
lein clean
lein with-profile prod cljsbuild once min
```
