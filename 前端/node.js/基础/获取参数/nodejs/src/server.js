var http = require('http'); //引入 http 模块
var url = require('url'); //引入url模块
var querystring = require('querystring'); //引入querystring库

function service(request, response) {

	//获取返回的url对象的query属性值
	var arg = url.parse(request.url).query;

	//将arg参数字符串反序列化为一个对象
	var params = querystring.parse(arg);

	//请求的方式
	console.log("method - " + request.method);

	//请求的url
	console.log("url - " + request.url)

	//设置返回代码200，以及返回格式为 text/plain
	response.writeHead(200, {
		'Content-Type': 'text/plain'
	});
	response.end('Hello Node.js');
}

var server = http.createServer(service); //基于service函数来创建服务器
server.listen(8088); //服务器监听于8088端口