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
            node.nodes = [];
            if (node.isExpanded) {
                $http.get('api/gettree/' + node.id).then(function (response) {
                    console.log(response.data.result);
                    node.nodes = response.data.result;
                });
            }
        };

        catalog.editNode = function (node) {
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
            catalog.activeNode.fields = [];
            catalog.activeNode = {};
            catalog.newNode = {};
            var classList = document.getElementById('input-name-field').classList;
            classList.remove('ng-dirty');
            classList.add('ng-pristine');
        };

        catalog.saveNode = function () {
            if (catalog.isAdding) {
                if (!confirm('Сохранить ' + catalog.newNode.name + '?')) return;
                catalog.isFormShow = false;
                $http.put('api/add', catalog.newNode).then(function (response) {
                    var parent = catalog.getParent(catalog, catalog.newNode);
                    if (parent.isExpanded) {
                        parent.nodes.push(response.data.result);
                    }
                    catalog.newNode = {};
                    console.log(response.data.result);
                    var classList = document.getElementById('input-name-field').classList;
                    classList.remove('ng-dirty');
                    classList.add('ng-pristine');
                });
            } else {
                if (!confirm('Сохранить изменения в ' + catalog.activeNode.name + '?')) return;
                catalog.isFormShow = false;
                var fields = catalog.activeNode.fields;
                var newFields = catalog.newNode.fields;
                for (var i = 0; i < fields.length; i++) {
                    var founded = false;
                    for(var j = 0; j < newFields.length; j++) {
                        if (fields[i].id === newFields[j].id) {
                            founded = true;
                        }
                    }
                    if (!founded) {
                        $http.delete('api/removefield/' + fields[i].id);
                    }
                }
                console.log(JSON.stringify(catalog.newNode));
                $http.put('api/update', catalog.newNode).then(function (response) {
                    var node = response.data.result;
                    console.log(node);
                    catalog.activeNode.parentId = node.parentId;
                    catalog.activeNode.name = node.name;
                    catalog.activeNode.fields = [];
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
            catalog.newNode.fields = [];
        };

        catalog.removeNode = function (node) {
            if (!confirm('Удалить запись ' + node.name + '?')) return;
            $http.delete('api/remove/' + node.id).then(function (response) {
                if (response.data.result === true) {
                    var parent = catalog.getParent(catalog, node);
                    var index = parent.nodes.indexOf(node);
                    parent.nodes.splice(index, 1);
                }
            });
        };

        catalog.addField = function () {
            catalog.newNode.fields.push({
                'nodeId': catalog.newNode.id,
                'name': '',
                'value': ''
            });
        };

        catalog.removeField = function (field) {
            var index = catalog.newNode.fields.indexOf(field);
            catalog.newNode.fields.splice(index, 1);
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
