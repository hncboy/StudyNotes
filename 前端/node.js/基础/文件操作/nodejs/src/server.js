//服务器模块
var http = require("http")
var url = require("url")

function start(route, handle) {
	function onRequest(request, response) {
		//首先获取访问路径pathname
		var pathname = url.parse(request.url).pathname
		//然后调用router.js的route函数，并把pathname 和 handle数组传递进去
		var html = route(handle, pathname)
		response.writeHead(200, {
			"Content-Type": "text/plain"
		});
		response.write(html)
		response.end()
	}
	
	http.createServer(onRequest).listen(8088)
}

exports.start = start;