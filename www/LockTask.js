var exec = require('cordova/exec');

exports.lockLauncher = function(enabled, success, error) {
    exec(null, error, 'LockTask', 'setLocked', [!!enabled]);
};

exports.isLocked = function(arg0, success, error) {
    exec(success, null, 'LockTask', 'isLocked', null);
};

