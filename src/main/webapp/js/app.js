(function () {
    var app = angular.module('catalog', []);

    app.controller('nodeController', ['$scope', '$http', function ($scope, $http) {
        var catalog = this;
        $scope.node = catalog;

        catalog.addedNodes = [];
        catalog.activeNode = {};
        catalog.newNode = {};
        catalog.oldNodes = [];
        catalog.nodes = [];

        catalog.isExpanded = true;
        catalog.isAdding = false;
        catalog.isFormShow = false;

        catalog.isFind = false;
        catalog.findNodes = [];
        catalog.findQuery = '';
        catalog.findTimer = {};

        catalog.isParentFormShow = false;
        catalog.newParent = {};

        catalog.treePage = 0;
        catalog.isTreeEnd = false;
        catalog.findPage = 0;
        catalog.isFindEnd = false;

        catalog.loading = false;
        
        catalog.sortDESC = false;
        catalog.sortedNodes = [];
        //window.catalog = catalog;

        catalog.treeLoadTimer = {};

        catalog.loadNodes = function () {
            $http.get('api/nodes/0/page/' + catalog.treePage).then(function (response) {
                console.log(response.data);
                if (response.data.result.length < 25) {
                    catalog.isTreeEnd = true;
                }
                for (var i = 0; i < response.data.result.length; i++) {
                    for (var j = 0; j < catalog.addedNodes.length; j++) {
                        if (response.data.result[i].id === catalog.addedNodes[j].id) {
                            var index = catalog.nodes.indexOf(catalog.addedNodes[j]);
                            catalog.nodes.splice(index, 1);
                            catalog.addedNodes.splice(j, 1);
                        }
                    }
                }
                catalog.nodes = catalog.nodes.concat(response.data.result);
                if (!catalog.isTreeEnd && window.innerHeight >= document.body.offsetHeight) {
                    clearTimeout(catalog.treeLoadTimer);
                    catalog.treeLoadTimer = setTimeout(function () {
                        if (!catalog.isTreeEnd && window.innerHeight >= document.body.offsetHeight) {
                            catalog.treePage++;
                            catalog.loadNodes();
                        }
                    }, 500);
                }
                catalog.sortNodes(catalog);
                catalog.loading = false;
            });
        };

        catalog.expand = function (node) {
            if (node.childrenCount === 0) return;
            node.isExpanded = !node.isExpanded;
            node.nodes = [];
            if (node.isExpanded) {
                $http.get('api/nodes/' + node.id + '/page/0').then(function (response) {
                    console.log(response.data);
                    node.nodes = response.data.result;
                    /*if (node.nodes.length === 0) {
                        node.isEmpty = true;
                    }*/
                });
            }
        };

        catalog.selectNewParent = function () {
            catalog.isFormShow = false;
            if (catalog.isFind) {
                catalog.findNodes = catalog.nodes;
                catalog.nodes = catalog.oldNodes;
            }
            catalog.isParentFormShow = true;
        };

        catalog.setParent = function (node) {
            if (typeof node === 'undefined') {
                catalog.newNode.parentId = 0;
                catalog.newParent = catalog;
            } else {
                catalog.newNode.parentId = node.id;
                console.log(node);
                catalog.newParent = node;
            }
            if (catalog.isFind) {
                catalog.nodes = catalog.findNodes;
            }
            catalog.isFormShow = true;
            catalog.isParentFormShow = false;
        };

        catalog.cancelParent = function () {
            if (catalog.isFind) {
                catalog.nodes = catalog.findNodes;
            }
            catalog.isFormShow = true;
            catalog.isParentFormShow = false;
        };

        catalog.editNode = function (node) {
            catalog.isAdding = false;
            $http.get('api/node/' + node.id).then(function (response) {
                node.fields = response.data.result.fields;
                catalog.newNode = JSON.parse(JSON.stringify(node));
                catalog.activeNode = node;
                catalog.isFormShow = true;
            });
        };

        catalog.find = function () {
            if (catalog.findQuery === '') return;
            catalog.isFind = true;
            catalog.isFindEnd = false;
            catalog.findPage = 0;
            if (catalog.oldNodes.length === 0) {
                catalog.oldNodes = catalog.nodes;
            }
            catalog.nodes = [];
            catalog.loadFindNodes();
        };

        catalog.loadFindNodes = function () {
            $http.get('api/node/find/page/' + catalog.findPage + '?query=' + catalog.findQuery).then(function (response) {
                console.log(response.data);
                if (response.data.result.length < 25) {
                    catalog.isFindEnd = true;
                }
                catalog.nodes = catalog.nodes.concat(response.data.result);
                if (!catalog.isFindEnd && window.innerHeight >= document.body.offsetHeight) {
                    catalog.findPage++;
                    catalog.loadFindNodes();
                }
                catalog.loading = false;
            });
        };

        catalog.clearFind = function () {
            catalog.loading = true;
            catalog.findPage = 0;
            catalog.findQuery = '';
            if (catalog.oldNodes.length !== 0) {
                catalog.nodes = [];
                catalog.nodes = catalog.oldNodes;
                catalog.oldNodes = [];
            }
            catalog.isFind = false;
            catalog.isFindEnd = false;
            catalog.loading = false;
        };

        catalog.findChange = function () {
            clearTimeout(catalog.findTimer);
            catalog.findTimer = setTimeout(catalog.find, 500);
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
                //console.log(catalog.newNode);
                catalog.isFormShow = false;
                $http.put('api/node', catalog.newNode).then(function (response) {
                    //console.log(response.data.result);
                    var parent = catalog.getParent(catalog, catalog.newNode.parentId);
                    parent.childrenCount++;
                    if (parent.isExpanded) {
                        parent.nodes.push(response.data.result);
                        catalog.sortNodes(parent);
                    }
                    if (parent === catalog) {
                        catalog.addedNodes.push(response.data.result);
                    }

                    catalog.newNode = {};
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
                        $http.delete('api/field/' + fields[i].id);
                    }
                }
                $http.post('api/node/' + catalog.newNode.id, catalog.newNode).then(function (response) {
                    var node = response.data.result;
                    //console.log(node);
                    if (catalog.activeNode.parentId !== node.parentId) {
                        var oldParent = catalog.getParent(catalog, catalog.activeNode.parentId);
                        var index = oldParent.nodes.indexOf(catalog.activeNode);
                        oldParent.nodes.splice(index, 1);
                        if (catalog.newParent.isExpanded) {
                            catalog.newParent.nodes.push(node);
                        }
                        catalog.activeNode.parentId = node.parentId;
                    } else {
                        catalog.activeNode.name = node.name;
                        catalog.activeNode.fields = [];
                    }
                    catalog.activeNode = {};
                    catalog.newNode = {};
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
            catalog.newNode.childrenCount = 0;
        };

        catalog.removeNode = function (node) {
            if (!confirm('Удалить запись ' + node.name + '?')) return;
            $http.delete('api/node/' + node.id).then(function (response) {
                if (response.data.result === true) {
                    var parent = catalog.getParent(catalog, node.parentId);
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

        catalog.getParent = function (parent, parentId) {
            if (parentId === 0) return catalog;
            if (parent.id === parentId) return parent;
            if (typeof parent.nodes === 'undefined') return false;
            for (var i = 0; i < parent.nodes.length; i++) {
                var p = catalog.getParent(parent.nodes[i], parentId);
                if (p !== false) {
                    return p;
                }
            }
            return false;
        };

        catalog.loadMore = function () {
            if (catalog.loading) return;
            if (catalog.isFind && catalog.isFindEnd) return;
            if (!catalog.isFind && catalog.isTreeEnd) return;
            catalog.loading = true;
            if (catalog.isFind) {
                catalog.findPage++;
                catalog.loadFindNodes();
            } else {
                catalog.treePage++;
                catalog.loadNodes();
            }
        };

        window.onscroll = function(ev) {
            if ((window.innerHeight + window.pageYOffset) >= document.body.offsetHeight) {
                catalog.loadMore();
            }
        };

        catalog.setSort = function (node) {
            node.sortDESC = !node.sortDESC;
            catalog.sortNodes(node);
        };
        
        catalog.compareDESC = function (a, b) {
            if (a.name.toLowerCase() < b.name.toLowerCase()) return 1;
            if (a.name.toLowerCase() > b.name.toLowerCase()) return -1;
            return 0;
        };

        catalog.compareASC = function (a, b) {
            if (a.name.toLowerCase() < b.name.toLowerCase()) return -1;
            if (a.name.toLowerCase() > b.name.toLowerCase()) return 1;
            return 0;
        };

        catalog.sortNodes = function (node) {
            if (node.sortDESC) {
                node.nodes.sort(catalog.compareDESC);
            } else {
                node.nodes.sort(catalog.compareASC);
            }
        };

        catalog.loadNodes();
    }]);

    app.directive('nodeTree', function () {
        return {
            restrict: 'A',
            templateUrl: 'node.html'
        };
    });

    app.directive('wireTree', function () {
        return {
            restrict: 'A',
            templateUrl: 'wire.html'
        };
    });
})();
