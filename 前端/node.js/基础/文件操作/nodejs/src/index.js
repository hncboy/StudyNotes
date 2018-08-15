/**
 * 路由概念
 * 如果没有路由的概念，那么无论是访问/listCategory路径 还是访问 /listProduct 路径，
 * 都是在service(request,response) 函数里做的。那么引入路由的概念的话，
 * 就是指访问 /listCategory 路径，会访问 listCategory函数。 而访问 /listProduct 路径，
 * 就会访问 listProduct 函数，这样子维护起来就容易多了。
 * 
 * 
 * 1.通过 node index.js 启动服务器
 * 2.index.js调用了server.start函数，并且传递了router.js里route函数和handle数组作为参数
 * 3.server.js 通过了8088端口启动了服务，然后用onRequest函数来处理业务
 * 	3.1 在 onRequest 中，首先获取访问路径 pathname
 * 	3.2 然后调用 router.js 的route 函数，并把pathname 和 handle数组传递进去
 * 4.在router.js 中，通过pathname为下标获取调用真正的业务函数，并把业务函数的返回值返回出去。
 * 	4.1 如果找不到，比如访问 /listUser 这个路径就没有在 handle 数组中找到对应，那么就会返回 listUser is not defined
 * 5. 当访问地址是 /listCategory的时候， 真正的业务函数 requestHandlers.js 中的 listCategory() 就会被调用，并返回业务 Html 代码 : "a lots of categorys".
 */

//入口主模块
var server = require("./server")
var router = require("./router")
var requestHandlers = require("./requestHandlers")

var handle = {}
handle["/listCategory"] = requestHandlers.listCategory
handle["/listProduct"] = requestHandlers.listProduct
handle["/readFile"] = requestHandlers.readFile
handle["/writeFile"] = requestHandlers.writeFile
//调用了server.start函数，并且传递了router.js里route函数和handle数组作为参数
server.start(router.route, handle)