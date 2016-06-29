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

First start [tunnels](https://github.com/jugyo/tunnels) and run
```
tunnels 34490 3449
```
and open [https://localhost:34490](https://localhost:34490) in your browser to accept tunnels' unverified TLS certificate. (This browser thing has to be done only once.)

Then,
```
lein clean
lein figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

### Start Horizon
Generate keys if needed with `hz create-cert`. Then,

```
$ hz serve --dev --permissions yes --secure yes --serve-static resources/public
```

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
