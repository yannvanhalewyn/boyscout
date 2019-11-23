# Boyscout ⛺

Boyscout is a Clojurescript rewrite of Clement Mihailescu's [Pathfinding Visualizer](https://github.com/clementmihailescu/Pathfinding-Visualizer/). All credit's for the idea and sexy animations sould go to him 🎓. Boyscout can visualize a couple of famous pathfinding algorithms for fun and educational purpouses.

View Boyscout in action on: [https://yannvanhalewyn.github.io/boyscout/](https://yannvanhalewyn.github.io/boyscout/) and after playing around a bit click on the Boyscout logo in the header for a hidden feature 😉.

This project has been built to practice algorithms, demonstrate the power of a declarative functional approach and most importantly to have some fun.

Some nice technical features to check out:

- A [board model](https://github.com/yannvanhalewyn/boyscout/blob/master/src/bs/board.cljs#L1) as a weighted bigraph
- An [algorithm abastraction](https://github.com/yannvanhalewyn/boyscout/blob/master/src/bs/algorithm.cljs#L1), think adapter pattern but not really. It's just some functions.
- A simple [animation system](https://github.com/yannvanhalewyn/boyscout/blob/master/src/bs/animation.cljs#L1) built with [core.async](https://clojure.github.io/core.async/) that can be stopped, rewinded and is open for more features (stepping feature? 🤔)
- Using clojure meta-data to [link straight to source](https://github.com/yannvanhalewyn/boyscout/blob/master/src/bs/algorithm.cljs#L8) in the UI

## Running

Clone the repo and the first time run:

    $ yarn install
    $ make css

Then to start and watch the test and dev build and start a nRepl server:

    $ make dev

And visit [localhost:8080](http://localhost:8080) to see the app, and [localhost:8081](http://localhost:8081) to se the test runner.

**Note:** you won't see a repl appear with this command. A nRepl server was started and you should connect to it with your favorite editor. If that's not an option you can run `make repl` to have a terminal repl. This won't build the test app though.

To watch the changes in css run:

    $ make css-watch

## Building

To build the project with all optimisations run:

    $ make build

And open the *index.html* file in the root of the project.
