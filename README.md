# om-next-e2e

This code is based on Mariano Guerra's blog posts:
* [Om.next end to end - Part I: Backend](http://marianoguerra.org/posts/omnext-end-to-end-part-i-backend.html)
* [Om.next end to end - Part II: Frontend](http://marianoguerra.org/posts/omnext-end-to-end-part-ii-frontend.html)

I add here ability to update connected clients when server state changes. I.e. when you look at the page with counter value, and someone updates the counter (either via UI or via curl), the value of the counter should change in the UI without refreshing the page.

# how to run it

Assuming you use Emacs+cider, and you configured your cider to start figwheel repl like this:

```
(setq cider-cljs-lein-repl "(do (use 'figwheel-sidecar.repl-api) (start-figwheel!) (cljs-repl))")
```
You can open the `project.clj` in emacs and hit `Ctrl-c Alt-Shift-j` to start both clojure and clojurescript REPLs. Then, in the clojure REPL, write `(reset)`, it will start the backend on 8080.

Then navigate to http://localhost:8080, you should see the page with 'Increment' button. When you click on the button, the value of the "Counter" increments.

Now, run following in terminal:

```
echo '[(ui/increment {:value 20})]' | transito http post http://localhost:8080/api/1/query e2t -
```

You should see that the value of "Counter" increments by 20.
