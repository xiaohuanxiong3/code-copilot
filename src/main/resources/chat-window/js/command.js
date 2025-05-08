// 用户输入触发 command 相关事件
userInput.addEventListener('input', function (event) {
    adjustTextareaHeight(userInput);
    const value = event.target.value;
    const cursorPosition = event.target.selectionStart;
    const currentChar = value[cursorPosition - 1];
    if (currentChar === '/') {
        commandList.querySelectorAll('.command-item').forEach(item => {
            item.style.display = 'block';
        });
        if (getComputedStyle(commandList).getPropertyValue('display') === 'none') {
            showCommandPopup();
        }
        initCommandSelection();
    } else {
        const lastSlashIndex = value.lastIndexOf('/');
        if (lastSlashIndex === -1) {
            commandList.style.display = 'none';
        } else {
            const query = value.substring(lastSlashIndex + 1);
            filterCommandList(query);
            initCommandSelection();
        }
    }
});

function showCommandPopup() {
    const { top, left } = getCaretCoordinates(userInput);

    // 设置提示框显示在光标右上角
    commandList.style.top = top + 'px';
    commandList.style.left = left + 'px';
    commandList.style.display = 'block';
}

function initCommandSelection() {
    currentCommandIndex = -1; // 重置当前命令索引
    userInput.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowDown' }));
}

function updateCommandSelection(items) {
    items.forEach((item, index) => {
        if (index === currentCommandIndex) {
            item.classList.add('selected');
            item.scrollIntoView({ block: 'nearest' });
        } else {
            item.classList.remove('selected');
        }
    });
}

function filterCommandList(query) {
    var num = 0;
    const items = commandList.querySelectorAll('.command-item');
    items.forEach(item => {
        if (item.textContent.toLowerCase().includes(query.toLowerCase())) {
            item.style.display = 'block';
            num++;
        } else {
            item.style.display = 'none';
        }
    });
    if (num === 0) {
        commandList.style.display = 'none';
        // 命令框隐藏时，将所有命令设为可见
        items.forEach(item => {
            item.style.display = 'block';
        });
    } else {
        commandList.style.display = 'block';
    }
}

// 用户按下上下方向键或回车键，触发事件
userInput.addEventListener('keydown', function (event) {
    const items = Array.from(commandList.querySelectorAll('.command-item')).filter(item => item.style.display !== 'none');
    if (items.length > 0 && commandList.style.display === 'block') {
        if (event.key === 'ArrowDown') {
            event.preventDefault();
            currentCommandIndex = (currentCommandIndex + 1) % items.length;
            updateCommandSelection(items);
        } else if (event.key === 'ArrowUp') {
            event.preventDefault();
            currentCommandIndex = (currentCommandIndex - 1 + items.length) % items.length;
            updateCommandSelection(items);
        } else if (event.key === 'Enter' && currentCommandIndex >= 0) {
            event.preventDefault();
            items[currentCommandIndex].click();
        }
    }
});

commandList.addEventListener('click', function (e) {
    if (e.target.classList.contains('command-item')) {
        removeCommandChars();
        resetCommandList();
        handleCommand(e.target.dataset.command);
        userInput.focus();  // 重新获取焦点
    }
});

// 删除以 / 开头的命令字符
function removeCommandChars() {
    setTimeout(() => {
        const lastSlashIndex = userInput.value.lastIndexOf('/');
        if (lastSlashIndex !== -1) {
            userInput.value = userInput.value.substring(0, lastSlashIndex);
        }
    }, 0);
}

// 重置所有命令项
function resetCommandList() {
    setTimeout(() => {
        commandList.querySelectorAll('.command-item').forEach(item => {
            item.style.display = 'block';
        });
        commandList.style.display = 'none';
    }, 0);
}

function handleCommand(command) {
    switch (command) {
        case 'tab':
            handleTabCommand();
            break;
        case 'select-file':
            addContextWidget('📄', '文件名.txt', '文件内容', false);
            break;
        case 'terminal':
            break;
        case 'diagnostics':
            break;
        case 'clear':
            messageWindow.innerHTML = '';
            handleClearCommand()
            break;
        // 可以添加更多的快捷指令处理逻辑
    }
}

// 默认实现为空，需要覆盖
function handleTabCommand() {

}

function handleClearCommand() {

}


