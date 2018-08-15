//路由模块

/**
 * 
 * @param {Object} handle 数组
 * @param {Object} pathname 路径
 */
function route(handle, pathname) {
	if(typeof handle[pathname] === 'function') {
		//通过pathname为下标获取调用真正的业务函数，并把业务函数的返回值返回出去
		return handle[pathname]();
	} else {
		//找不到则返回下面这句话
		return pathname + 'is not defind';
	}
}
exports.route = route;