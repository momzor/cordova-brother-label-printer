var BrotherPrinter = function () {}
BrotherPrinter.prototype = {
    findNetworkPrinters: function (callback, scope) {
        var callbackFn = function () {
            var args = typeof arguments[0] == 'boolean' ? arguments : arguments[0]
            callback.apply(scope || window, args)
        }
        cordova.exec(callbackFn, null, 'BrotherPrinter', 'findNetworkPrinters', [])
    },
    printViaSDK: function (data, callback) {
        if (!data || !data.length) {
            console.log('No data passed in. Expects a bitmap.')
            return
        }
        cordova.exec(callback, function(err){console.log('error: '+err)}, 'BrotherPrinter', 'printViaSDK', [data])
    },
    commandViaUSB: function (data, callback) {
        if (!data || !data.length) {
            console.log('No data passed in. Expects print payload string.')
            return
        }
        cordova.exec(callback, function(err){console.log('error: '+err)}, 'BrotherPrinter', 'commandViaUSB', [data])
    }
}
var plugin = new BrotherPrinter()
module.exports = plugin
