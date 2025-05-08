package com.github.xiaohuanxiong3.codecopilot.support.jcef

import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.*
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefMessageRouter
import org.cef.browser.CefMessageRouter.CefMessageRouterConfig
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefMessageRouterHandler
import org.cef.handler.CefMessageRouterHandlerAdapter
import org.cef.misc.CefLog
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.function.Function

/**
 * 对内置的 JBCefJSQuery做了修改，在需要 handler 异步执行完成之后执行回调时使用此类更友好
 * 但此类无法和 JBCefJSQuery 共享 myJSQueryCounter，需谨慎使用
 * 且此类只能在 CefBrowser 创建之前初始化，否则可能会有预料之外的错误
 * @Author Handsome Young
 * @Date 2024/10/13 02:13
 */
class AsyncJBCefJSQuery private constructor(browser: JBCefBrowserBase, func: JSQueryFunc) : JBCefDisposable {
    private val myFunc = func
    private val myJBCefClient = browser.jbCefClient
    private val myIsDisposed = AtomicBoolean(false)
    private val myHandlerMap: MutableMap<Function<in HandlerParam, out Response?>, CefMessageRouterHandler> =
        Collections.synchronizedMap(
            HashMap()
        )

    init {
        Disposer.register(browser.jbCefClient, this)
    }

    /**
     * @return name of the global function JS must call to send query to Java
     */
    fun getFuncName(): String {
        return myFunc.myFuncName
    }

    /**
     * Returns the query callback to inject into JS code.
     *
     * @param queryResult the result (JS variable name, or JS value in single quotes) that will be passed to the Java handler [.addHandler]
     * @param onSuccessCallback JS callback in format: `function(response) {}`
     * @param onFailureCallback JS callback in format: `function(error_code, error_message) {}`
     */
    /**
     * Returns the query callback to inject into JS code.
     *
     * @param queryResult the result (JS variable name, or JS value in single quotes) that will be passed to the Java handler [.addHandler]
     */
    @JvmOverloads
    fun inject(
        queryResult: String?,
        onSuccessCallback: String = "function(response) {}",
        onFailureCallback: String = "function(error_code, error_message) {}"
    ): String {
        var queryResult = queryResult
        checkDisposed()

        if (queryResult != null && queryResult.isEmpty()) queryResult = "''"
        return "window." + myFunc.myFuncName +
                "({request: '' + " + queryResult + "," +
                "onSuccess: " + onSuccessCallback + "," +
                "onFailure: " + onFailureCallback +
                "});"
    }

    fun addHandler(handler: Function<in HandlerParam, out Response?>) {
        checkDisposed()

        var cefHandler: CefMessageRouterHandler
        myFunc.myRouter.addHandler(object : CefMessageRouterHandlerAdapter() {
            override fun onQuery(
                browser: CefBrowser,
                frame: CefFrame,
                query_id: Long,
                request: String,
                persistent: Boolean,
                callback: CefQueryCallback
            ): Boolean {
                if (DEBUG_JS) CefLog.Debug(
                    "onQuery: browser=%s, frame=%s, qid=%d, request=%s",
                    browser,
                    frame,
                    query_id,
                    request
                )
                handler.apply(HandlerParam(request, callback))
                //                if (callback != null && response != null) {
//                    if (response.isSuccess() && response.hasResponse()) {
//                        callback.success(response.response());
//                    } else {
//                        callback.failure(response.errCode(), response.errMsg());
//                    }
//                } else if (callback != null) {
//                    callback.success("");
//                }
                return true
            }

            override fun onQueryCanceled(browser: CefBrowser, frame: CefFrame, queryId: Long) {
                if (DEBUG_JS) CefLog.Debug("onQueryCanceled: browser=%s, frame=%s, qid=%d", browser, frame, queryId)
            }
        }.also { cefHandler = it }, false)
        myHandlerMap[handler] = cefHandler
    }

    fun removeHandler(function: Function<in HandlerParam, out Response?>) {
        val cefHandler = myHandlerMap.remove(function)
        if (cefHandler != null) {
            myFunc.myRouter.removeHandler(cefHandler)
        }
    }

    fun clearHandlers() {
        val functions: MutableList<Function<in HandlerParam, out Response?>> =
            ArrayList(myHandlerMap.size)
        // Collection.synchronizedMap object is the internal mutex for the collection.
        synchronized(myHandlerMap) {
            myHandlerMap.forEach { (func: Function<in HandlerParam, out Response?>?, handler: CefMessageRouterHandler?) ->
                func?.let {
                    functions.add(
                        it
                    )
                }
            }
        }
        functions.forEach(Consumer { func: Function<in HandlerParam, out Response?> -> removeHandler(func) })
    }

    override fun dispose() {
        if (!myIsDisposed.getAndSet(true)) {
            myJBCefClient.cefClient.removeMessageRouter(myFunc.myRouter)
            myFunc.myRouter.dispose()
            myHandlerMap.clear()
        }
    }

    override fun isDisposed(): Boolean {
        return myIsDisposed.get()
    }

    private fun checkDisposed() {
        check(!isDisposed) { "the JS query has been disposed" }
    }

    /**
     * A JS handler response to a query.
     */
    class Response @JvmOverloads constructor(
        private val myResponse: String?,
        private val myErrCode: Int = ERR_CODE_SUCCESS,
        private val myErrMsg: String? = null
    ) {
        fun response(): String? {
            return myResponse
        }

        fun errCode(): Int {
            return myErrCode
        }

        fun errMsg(): String? {
            return myErrMsg
        }

        val isSuccess: Boolean
            get() = myErrCode == ERR_CODE_SUCCESS

        fun hasResponse(): Boolean {
            return myResponse != null
        }

        companion object {
            const val ERR_CODE_SUCCESS: Int = 0
        }
    }

    internal class JSQueryFunc @JvmOverloads constructor(
        client: JBCefClient,
        index: Int = myJSQueryCounter.incrementAndGet(),
        isSlot: Boolean = false
    ) {
        val myRouter: CefMessageRouter
        val myFuncName: String
        val myIsSlot: Boolean

        init {
            val postfix = client.hashCode().toString() + "_" + (if (isSlot) "slot_" else "") + index
            myIsSlot = isSlot
            myFuncName = "cefQuery_$postfix"
            val config = CefMessageRouterConfig()
            config.jsQueryFunction = myFuncName
            config.jsCancelFunction = "cefQuery_cancel_$postfix"
            myRouter = JBCefApp.getInstance().createMessageRouter(config)
            client.cefClient.addMessageRouter(myRouter)
        }
    }

    companion object {
        private val DEBUG_JS = java.lang.Boolean.getBoolean("ide.browser.jcef.debug.js")

        private val myJSQueryCounter = AtomicInteger(100)

        /**
         * Creates a unique JS query.
         *
         * @param browser the associated CEF browser
         * @see JBCefClient.Properties.JS_QUERY_POOL_SIZE
         */
        fun create(browser: JBCefBrowserBase): AsyncJBCefJSQuery {
            val create =
                Function { v: Void? -> AsyncJBCefJSQuery(browser, JSQueryFunc(browser.jbCefClient)) }
            return create.apply(null)
        }

        @Deprecated("use {@link #create(JBCefBrowserBase)}")
        fun create(browser: JBCefBrowser): AsyncJBCefJSQuery {
            return create(browser as JBCefBrowserBase)
        }
    }
}
