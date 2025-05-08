function sendMessage() {
    const message = userInput.value.trim();
    if (message) {
        handleBeforeChat();
        // let context = buildApplyCodeBetaMessages();
        appendUserMessage(message);
        appendAIElement();
        chatWindow.lastElementChild.scrollIntoView({ behavior: "smooth", block: "end" });
        chat(message);
    }
}

function handleBeforeChat() {
    showStopResponseButton = true;
    document.getElementById('stop-button-container').style.display = 'block';
    document.getElementById("chat-input").style.display = 'none';
}

function handleAfterChat() {
    showStopResponseButton = false;
    document.getElementById('stop-button-container').style.display = 'none';
    document.getElementById("chat-input").style.display = 'block';
    adjustTextareaHeight(userInput, true); // 发送消息后将用户输入框调整为默认高度
}

function appendUserMessage(message) {
    const messageElement = document.createElement("div");
    messageElement.classList.add("chat-message", "user");
    messageElement.innerHTML = message.replace(/\n/g, '<br>'); // 将换行符转换为 <br>
    attachContextInfo();
    messageWindow.appendChild(messageElement);
    userInput.value = "";
}

// 将上下文信息添加到用户消息中，并清空相关的元素
function attachContextInfo() {
    fileContainer.innerHTML = ''; // 清空文件容器
}

function appendAIElement() {
    const messageElement = document.createElement("div");
    messageElement.classList.add("chat-message", "ai");
    messageWindow.appendChild(messageElement);
}

function chat(message) {
    doChat(message);
}

function streamResponse(htmlResponse) {
    messageWindow.lastElementChild.innerHTML = formatMessage(htmlResponse);
    handleCodeBlocks();
}

// doChat默认实现，需要覆盖
function doChat(message) {

}

userInput.addEventListener("keypress", function (e) {
    if (e.key === "Enter" && !e.shiftKey) {
        // 添加 !e.shiftKey 条件
        e.preventDefault(); // 防止换行
        sendMessage();
    }
});

sendButton.addEventListener("click", function () {
    sendMessage();
});

function stopChat() {
    showStopResponseButton = false;
    document.getElementById('stop-button-container').style.display = 'none';
    doStopChat();
}

function doStopChat() {

}

selectModelElement.addEventListener("change", function () {
    const model = this.value;
    handleModelChange(model);
});

function handleModelChange(model) {
    // do something
}



