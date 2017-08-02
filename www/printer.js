var BrotherPrinter = function () {}
BrotherPrinter.prototype = {

    printViaBluetooth: function (data, numberOfCopies, callback) {
        if (!data || !data.length) {
            console.log('No data passed in. Expects a bitmap.')
            return
        }
        cordova.exec(callback, function(err){console.log('error: '+err)}, 'BrotherPrinter', 'printViaBluetooth', [data, numberOfCopies])
    },

    printViaNetwork: function (data, numberOfCopies, callback) {
        if (!data || !data.length) {
            console.log('No data passed in. Expects a bitmap.')
            return
        }
        cordova.exec(callback, function(err){console.log('error: '+err)}, 'BrotherPrinter', 'printViaNetwork', [data, numberOfCopies])
    }

}
var plugin = new BrotherPrinter()
module.exports = plugin
