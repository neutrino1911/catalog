(function () {
    var app = angular.module('catalog', []);

    app.controller('nodeController', ['$http', function ($http) {
        var catalog = this;

        catalog.activeNode = {};
        catalog.newNode = {};
        catalog.nodes = [];

        catalog.isAdding = false;

        $http.get('api/gettree/0').then(function (response) {
            catalog.nodes = response.data.result;
        });

        catalog.expand = function (node) {
            node.isExpanded = !node.isExpanded;
            if (node.isExpanded && !node.nodes) {
                node.nodes = [];
                $http.get('api/gettree/' + node.id).then(function (response) {
                    node.nodes = response.data.result;
                });
            }
        };

        catalog.edit = function (node) {
            catalog.isAdding = false;
            $http.get('api/get/' + node.id).then(function (response) {
                node.fields = response.data.result.fields;
                catalog.newNode = JSON.parse(JSON.stringify(node));
                catalog.activeNode = node;
                catalog.isFormShow = true;
            });
        };

        catalog.cancel = function () {
            if (!confirm('Закрыть без сохранения?')) return;
            catalog.isFormShow = false;
            catalog.activeNode = [];
            catalog.newNode = [];
            var classList = document.getElementById('input-name-field').classList;
            classList.remove('ng-dirty');
            classList.add('ng-pristine');
        };

        catalog.saveNode = function () {
            if (catalog.isAdding) {
                if (!confirm('Сохранить ' + catalog.newNode.name + '?')) return;
                $http.put('api/add', catalog.newNode).then(function (response) {
                    catalog.isFormShow = false;
                    var parent = catalog.getParent(catalog, catalog.newNode);
                    if (parent.isExpanded && typeof parent.nodes === 'undefined') {
                        parent.nodes = [];
                    }
                    if (typeof parent.nodes !== 'undefined') {
                        parent.nodes.push(catalog.newNode);
                    }
                    catalog.newNode = {};
                    console.log(catalog.newNode);
                    var classList = document.getElementById('input-name-field').classList;
                    classList.remove('ng-dirty');
                    classList.add('ng-pristine');
                });
            }else {
                if (!confirm('Сохранить изменения в ' + catalog.newNode.name + '?')) return;
                $http.put('api/update', catalog.newNode).then(function (response) {
                    catalog.isFormShow = false;
                    catalog.activeNode.name = catalog.newNode.name;
                    catalog.activeNode.fields = catalog.newNode.fields;
                    var classList = document.getElementById('input-name-field').classList;
                    classList.remove('ng-dirty');
                    classList.add('ng-pristine');
                });
            }
        };

        catalog.addNode = function (parentNode) {
            catalog.isAdding = true;
            catalog.isFormShow = true;
            catalog.newNode.parentId = parentNode.id;
        };

        catalog.removeNode = function (node) {
            if (!confirm('Удалить запись ' + node.name + '?')) return;
            $http.delete('api/remove/' + node.id).then(function (response) {
                console.log(response);
                if (response.data.result === true) {
                    var parent = catalog.getParent(catalog, node);
                    var index = parent.nodes.indexOf(node);
                    parent.nodes.splice(index, 1);
                }
            });
        };

        catalog.getParent = function (parent, node) {
            if (node.parentId === 0) return catalog;
            if (parent.id === node.parentId) return parent;
            if (typeof parent.nodes === 'undefined') return false;
            for (var i = 0; i < parent.nodes.length; i++) {
                var p = catalog.getParent(parent.nodes[i], node);
                if (p !== false) {
                    return p;
                }
            }
            return false;
        };
    }]);

    app.directive('nodeTree', function () {
        return {
            restrict: 'A',
            templateUrl: 'node.html'
        };
    });
})();
