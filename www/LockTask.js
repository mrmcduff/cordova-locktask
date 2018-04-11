var exec = require('cordova/exec');

exports.lockLauncher = function(enabled, success, error) {
    exec(success, error, 'LockTask', 'setLocked', [!!enabled]);
};

exports.isLocked = function(success, error) {
    exec(success, error, 'LockTask', 'isLocked', null);
};

