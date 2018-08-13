//引入 http 模块
var http = require('http');

function service(request, response) {
	//设置返回代码200，以及返回格式为 text/plain
	response.writeHead(200, {
		'Content-Type': 'text/plain'
	});
	response.end('Hello Node.js');
}
//基于service函数来创建服务器
var server = http.createServer(service);
//服务器监听于8088端口
server.listen(8088);