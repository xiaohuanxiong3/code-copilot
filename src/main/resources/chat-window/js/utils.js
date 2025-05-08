const aiResponse = `
<p>您遇到的问题是关于相对路径的资源引用在JCEF环境中无法正确解析。这是因为当您使用 <code>loadHTML</code> 方法加载HTML内容时，浏览器没有一个基础URL来解析这些相对路径。让我们来解决这个问题：</p>
<ol>
<li>理解问题</li>
</ol>
<p>当您的HTML中包含像 <code>&lt;script src=&quot;/js/highlight.min.js&quot;&gt;&lt;/script&gt;</code> 这样的相对路径时，浏览器不知道从哪里开始查找这些资源。在常规的Web环境中，这些路径通常相对于网站的根目录，但在JCEF中，没有这样的概念。</p>
<ol start="2">
<li>解决方案</li>
</ol>
<p>有几种方法可以解决这个问题：</p>
<p>a. 使用绝对路径</p>
<p>将相对路径改为绝对路径，指向您插件资源中的实际位置：</p>
<pre><code class="language-html">&lt;script src=&quot;plugins/your-plugin-id/js/highlight.min.js&quot;&gt;&lt;/script&gt;
&lt;link rel=&quot;stylesheet&quot; href=&quot;plugins/your-plugin-id/css/chat.css&quot;&gt;
</code></pre>
<p>b. 使用 data URI</p>
<p>对于小型文件，您可以将它们直接嵌入到HTML中：</p>
<pre><code class="language-java">public String getResourceAsDataUri(String resourcePath, String mimeType) throws IOException {
    try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
        byte[] bytes = inputStream.readAllBytes();
        String base64 = Base64.getEncoder().encodeToString(bytes);
        return &quot;data:&quot; + mimeType + &quot;;base64,&quot; + base64;
    }
}

// 在加载HTML之前
String highlightJs = getResourceAsDataUri(&quot;/js/highlight.min.js&quot;, &quot;application/javascript&quot;);
String chatCss = getResourceAsDataUri(&quot;/css/chat.css&quot;, &quot;text/css&quot;);

// 替换HTML中的引用
htmlContent = htmlContent.replace(&quot;src=\&quot;/js/highlight.min.js\&quot;&quot;, &quot;src=\&quot;&quot; + highlightJs + &quot;\&quot;&quot;);
htmlContent = htmlContent.replace(&quot;href=\&quot;/css/chat.css\&quot;&quot;, &quot;href=\&quot;&quot; + chatCss + &quot;\&quot;&quot;);
</code></pre>
<p>c. 使用 <code>loadHTMLWithBaseURL</code> 方法</p>
<p>如果您的JCEF版本支持，可以使用 <code>loadHTMLWithBaseURL</code> 方法：</p>
<pre><code class="language-java">URL baseUrl = getClass().getResource(&quot;/&quot;);
browser.loadHTMLWithBaseURL(baseUrl.toExternalForm(), htmlContent);
</code></pre>
<p>d. 动态注入脚本和样式</p>
<p>您可以在HTML加载后，使用JavaScript动态添加脚本和样式：</p>
<pre><code class="language-java">// 加载HTML后
String injectScriptCode = &quot;var script = document.createElement('script'); &quot; +
                          &quot;script.src = '&quot; + getResourceAsDataUri(&quot;/js/highlight.min.js&quot;, &quot;application/javascript&quot;) + &quot;';&quot; +
                          &quot;document.head.appendChild(script);&quot;;
browser.getCefBrowser().executeJavaScript(injectScriptCode, &quot;&quot;, 0);

String injectStyleCode = &quot;var link = document.createElement('link'); &quot; +
                         &quot;link.rel = 'stylesheet';&quot; +
                         &quot;link.href = '&quot; + getResourceAsDataUri(&quot;/css/chat.css&quot;, &quot;text/css&quot;) + &quot;';&quot; +
                         &quot;document.head.appendChild(link);&quot;;
browser.getCefBrowser().executeJavaScript(injectStyleCode, &quot;&quot;, 0);
</code></pre>
<ol start="3">
<li>
<p>实施步骤</p>
</li>
<li>
<p>选择上述方法之一（或组合使用）。</p>
</li>
<li>
<p>修改您的HTML加载代码。</p>
</li>
<li>
<p>确保所有资源（JS、CSS文件）都在正确的位置，并且路径正确。</p>
</li>
<li>
<p>使用JCEF的开发者工具（<code>browser.openDevtools()</code>）来检查资源是否正确加载。</p>
</li>
<li>
<p>注意事项</p>
</li>
</ol>
<ul>
<li>路径问题：确保资源文件的路径正确，并且在您的插件JAR或资源目录中可以找到。</li>
<li>性能考虑：对于大型文件，避免使用data URI方法，因为它会增加HTML的大小。</li>
<li>缓存：考虑实现缓存机制，特别是如果您频繁重新加载HTML内容。</li>
<li>安全性：确保只加载您信任的资源，避免潜在的安全风险。</li>
</ul>
<p>如果您还有任何问题或需要进一步的帮助，请随时告诉我。我可以帮助您调试具体的代码或提供更多细节。</p>
        `;

// 输入框自适应高度调整
function adjustTextareaHeight(textarea, reset = false) {
    if (reset) {
        textarea.style.height = defaultTextareaHeight;
    } else {
        textarea.style.height = 'auto';
        textarea.style.height = textarea.scrollHeight + 'px';
    }
}

function formatMessage(message) {
    // 使用DOMParser来解析HTML字符串
    const doc = parser.parseFromString(message, "text/html");

    // 返回处理后的HTML字符串
    return doc.body.innerHTML;
}

// 处理代码块
function handleCodeBlocks () {
    document.querySelectorAll("pre code").forEach((block) => {
        if (block.getAttribute("data-processed") !== "true") {
            hljs.highlightBlock(block);
            block.setAttribute("data-processed", "true"); // 标记为已处理
        }
        // 创建复制按钮
        const copyButton = document.createElement("span");
        copyButton.classList.add("code-block-button", "copy-button");
        copyButton.textContent = "复制";
        copyButton.addEventListener("click", () => {
            copyToClipboard(block.innerText, copyButton, "copy-button");
        });

        // 创建应用按钮
        const applyButton = document.createElement("span");
        applyButton.classList.add("code-block-button", "apply-button");
        applyButton.textContent = "应用";
        applyButton.addEventListener("click", () => {
            applyCode(block.innerText, applyButton, "apply-button");
        });

        // 创建应用（beta）按钮
        const applyBetaButton = document.createElement("span");
        applyBetaButton.classList.add("code-block-button", "apply-beta-button");
        applyBetaButton.textContent = "应用（beta）";
        applyBetaButton.addEventListener("click", () => {
            applyCodeBeta(block.innerText, applyBetaButton, "apply-beta-button");
        });

        block.parentElement.style.position = "relative"; // 确保父元素是相对定位

        // 将按钮添加到代码块
        block.parentElement.appendChild(applyBetaButton);
        block.parentElement.appendChild(applyButton);
        block.parentElement.appendChild(copyButton);

        // 添加鼠标离开事件监听器
        block.parentElement.addEventListener("mouseleave", () => {
            if (applyButton.dataset.applied === "true" || copyButton.dataset.applied === "true" || applyBetaButton.dataset.applied === "true") {
                // 移除对勾图标
                const applyBetaButtonCheckmark = block.parentElement.querySelector(".apply-beta-button.button-checkmark");
                if (applyBetaButtonCheckmark) applyBetaButtonCheckmark.remove();
                const applyButtonCheckmark = block.parentElement.querySelector(".apply-button.button-checkmark");
                if (applyButtonCheckmark) applyButtonCheckmark.remove();
                const copyButtonCheckmark = block.parentElement.querySelector(".copy-button.button-checkmark");
                if (copyButtonCheckmark) copyButtonCheckmark.remove();

                // 重置按钮状态
                applyBetaButton.classList.remove("applied");
                applyButton.classList.remove("applied");
                copyButton.classList.remove("applied");
                applyBetaButton.dataset.applied = "false";
                applyButton.dataset.applied = "false";
                copyButton.dataset.applied = "false";
            }
        });
    });
}

// 显示加载动画
function showSpinner(button, buttonSpecifiedClass) {
    const spinner = document.createElement("span");
    spinner.classList.add(buttonSpecifiedClass, "button-spinner");
    button.classList.add("applied") // 隐藏应用按钮
    button.parentElement.appendChild(spinner);
}

function afterApply(button, buttonSpecifiedClass) {
    console.log("afterApply start")
    // 移除加载图标，显示对勾图标
    button.parentElement.querySelector( '.' + buttonSpecifiedClass + '.button-spinner').remove();
    const checkmark = document.createElement("span");
    checkmark.classList.add(buttonSpecifiedClass, "button-checkmark");
    checkmark.innerHTML = "&#10003;"; // Unicode字符对勾
    button.parentElement.appendChild(checkmark);

    // 添加标记，表示操作已完成
    button.dataset.applied = "true";
    console.log("afterApply end")
}

function copyToClipboard(text, copyButton, buttonSpecifiedClass) {
    navigator.clipboard.writeText(text).then(() => {
        copyButton.classList.add("applied") // 隐藏复制按钮

        const checkmark = document.createElement("span");
        checkmark.classList.add(buttonSpecifiedClass, "button-checkmark");
        checkmark.innerHTML = "&#10003;"; // Unicode字符对勾
        copyButton.parentElement.appendChild(checkmark);

        // 添加标记，表示操作已完成
        copyButton.dataset.applied = "true";
    }).catch(err => {
        console.error("复制失败", err);
    });
}

function applyCode(code, applyButton, buttonSpecifiedClass) {
    showSpinner(applyButton, buttonSpecifiedClass);
    doApplyCode(code, applyButton, buttonSpecifiedClass);
}

// 默认为空实现，需要覆盖
function doApplyCode(code, applyButton, buttonSpecifiedClass) {

}

function applyCodeBeta(code, applyBetaButton, buttonSpecifiedClass) {
    showSpinner(applyBetaButton, buttonSpecifiedClass);
    doApplyCodeBeta(code, applyBetaButton, buttonSpecifiedClass);
}

function doApplyCodeBeta(code, applyBetaButton, buttonSpecifiedClass) {

}

// 获取光标在textarea中的相对位置
function getCaretCoordinates(textarea) {
    const position = userInput.selectionStart;

    const div = document.createElement('div');
    const copyStyle = getComputedStyle(textarea);

    for (const prop of copyStyle) {
        div.style[prop] = copyStyle[prop];
    }

    div.style.position = 'absolute';
    div.style.visibility = 'hidden';
    div.style.whiteSpace = 'pre-wrap';
    div.style.wordWrap = 'break-word';
    div.textContent = textarea.value.substring(0, position);

    const span = document.createElement('span');
    span.textContent = textarea.value.substring(position) || '.';
    div.appendChild(span);

    document.body.appendChild(div);

    const { offsetTop: top, offsetLeft: left, offsetHeight: height } = span;

    document.body.removeChild(div);

    // 返回光标的实际位置坐标
    return {
        top: top + textarea.offsetTop - textarea.scrollTop + height,  // 光标上方
        left: left + textarea.offsetLeft
    };
}

function addContextWidget(icon, name, content, isBase64 = true) {
    // 创建小组件容器
    const fileWidget = document.createElement('div');
    fileWidget.classList.add('file-widget');
    fileWidget.dataset.content = content; // 将文件内容存储在 data 属性中
    fileWidget.dataset.fileName = name; // 将文件名存储在 data 属性中

    // 创建文件图标
    const fileIcon = document.createElement('span');
    fileIcon.classList.add('file-icon');
    if (isBase64) {
        const img = document.createElement('img');
        img.src = icon; // 使用 base64 字符串作为图标
        // img.style.width = '24px'; // 设置图标宽度
        // img.style.height = '24px'; // 设置图标高度
        fileIcon.appendChild(img);
        fileWidget.appendChild(fileIcon);
    } else {
        fileIcon.textContent = '📄'; // 使用 emoji 作为文件图标
        fileWidget.appendChild(fileIcon);
    }

    // 创建文件名
    const fileName = document.createElement('span');
    fileName.classList.add('file-name');
    fileName.textContent = name; // 示例文件名
    fileWidget.appendChild(fileName);

    // 创建删除按钮
    const deleteButton = document.createElement('span');
    deleteButton.classList.add('delete-button');
    deleteButton.innerHTML = '&#10006;'; // 使用 Unicode 字符
    fileWidget.appendChild(deleteButton);

    // 为删除按钮添加事件监听器
    deleteButton.addEventListener('click', function () {
        fileWidget.remove();
    });

    // 将小组件添加到父容器中
    fileContainer.appendChild(fileWidget);

    // 动态设置文件名的最大宽度
    const chatInputWidth = fileContainer.offsetWidth;
    const iconWidth = fileIcon.offsetWidth;
    const buttonWidth = deleteButton.offsetWidth;
    const padding = 30; // 预留一些内边距
    fileName.style.maxWidth = (chatInputWidth - iconWidth - buttonWidth - padding) + 'px';
}

