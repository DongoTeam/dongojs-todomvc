import org.dongojs.Component
import org.dongojs.ComponentRouter
import org.dongojs.core.render.html.Html
import org.dongojs.core.store.Store
import org.dongojs.router.Location
import org.dongojs.router.Route
import org.dongojs.router.Router
import kotlin.properties.Delegates

class Todo(
    val value: String,
    private val update: () -> Unit,
    initComplete: Boolean = false
) {
    var completed: Boolean by Delegates.observable(initComplete) { _, old, new ->
        if (old != new) {
            update()
        }
    }

    fun copy(newInitComplete: Boolean) =
        Todo(
            value = value,
            initComplete = newInitComplete,
            update = update
        )
}

class TodoItem(
    private val todo: Todo,
    private val onRemove: () -> Unit
) : Component() {

    private val toggleStore = Store(todo.completed)
    private val hoveredStore = Store(false)

    init {
        toggleStore.subscribe {
            todo.completed = it
        }
    }


    override val render by lazy {
        html {
            div("view") {
                input.checkbox("toggle", toggleStore)
                add(Html("label", "").apply {
                    +todo.value
                })
                button("destroy") {
                    o clazz hoveredStore { css.oriShow(it) }
                    events.click { onRemove() }
                }

                events("mouseenter") {
                    hoveredStore.change(true)
                }
                events("mouseleave") {
                    hoveredStore.change(false)
                }
            }
        }
    }
}

class TodoList(
    todoListStore: Store<List<Todo>>,
    filter: (Todo) -> Boolean
) : ComponentRouter() {
    override val render by lazy {
        html {
            ul("todo-list") {
                generate(todoListStore) { todos ->
                    todos.filter(filter).reversed().forEach { todo ->
                        li {
                            if (todo.completed)
                                o clazz "completed"
                            +TodoItem(
                                todo = todo,
                                onRemove = {
                                    todoListStore.change {
                                        it - todo
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

class App : Component() {

    private val newTodoStore = Store("")
    private val toggleAllStore = Store(false)
    private val todoListStore = Store<List<Todo>>(listOf())

    init {
        todoListStore.subscribe {
            toggleAllStore.change(it.all { it.completed })
        }
    }

    private fun onAppendTodo() {
        newTodoStore.change { todo ->
            if (todo.isNotEmpty()) {
                todoListStore.change {
                    it + Todo(
                        initComplete = false,
                        value = todo,
                        update = {
                            todoListStore.update()
                        }
                    )
                }
            }
            ""
        }
    }

    private fun onChangeToggleAll() {
        todoListStore.change {
            it.map { it.copy(toggleAllStore.value) }
        }
    }

    override val render by lazy {
        html {
            section("todoapp") {
                div {
                    header("header") {
                        h1 {
                            +"todos"
                        }
                        input.text("new-todo", newTodoStore) {
                            o placeholder "What needs to be done?"
                            events.onEnter {
                                onAppendTodo()
                            }
                        }
                    }
                    section("main") {
                        input.checkbox("toggle-all", toggleAllStore) {
                            o clazz todoListStore {
                                css.oriHide(it.isEmpty())
                            }
                            events.change {
                                onChangeToggleAll()
                            }
                        }
                        div {
                            +Router(
                                Route("/") {
                                    TodoList(todoListStore) { true }
                                },
                                Route("/all") {
                                    TodoList(todoListStore) { true }
                                },
                                Route("/active") {
                                    TodoList(todoListStore) { !it.completed }
                                }
                                ,
                                Route("/completed") {
                                    TodoList(todoListStore) { it.completed }
                                }
                            )
                        }
                    }
                }
                footer("footer") {
                    o clazz todoListStore {
                        css.oriHide(it.isEmpty())
                    }
                    span("todo-count") {
                        strong {
                            generate(todoListStore) {
                                +(it.size.toString())
                            }
                        }
                        span { +" " }
                        span { +"item" }
                        span { +" left" }
                    }
                    ul("filters") {
                        li {
                            a {
                                +"All"
                                o clazz Location.hash {
                                    css(it == "#/all" || it == "", "selected")
                                }
                                events.click { Location.navigate("/all") }
                            }
                        }
                        li {
                            a {
                                +"Active"
                                o clazz Location.hash {
                                    css(it == "#/active", "selected")
                                }
                                events.click { Location.navigate("/active") }
                            }
                        }
                        li {
                            a {
                                +"Completed"
                                o clazz Location.hash {
                                    css(it == "#/completed", "selected")
                                }
                                events.click { Location.navigate("/completed") }
                            }
                        }
                    }
                }
            }
        }
    }
}