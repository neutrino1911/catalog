(function(){
    var app = angular.module('catalog', [ ]);


    app.controller('nodeController', [ '$http', function($http){
        var catalog = this;

        catalog.activeNode = [ ];
        catalog.newNode = [ ];
        catalog.nodes = [ ];
        $http.get('api/gettree/0').then(function(response){
            console.log(response.data);
            catalog.nodes = response.data.result;
        });

        catalog.expand = function(node){
            node.isExpanded = !node.isExpanded;
            if (node.isExpanded && !node.nodes) {
                node.nodes = [ ];
                $http.get('api/gettree/' + node.id).then(function(response){
                    console.log(response.data);
                    node.nodes = response.data.result;
                });
            }
        };

        catalog.edit = function(node){
            $http.get('api/get/' + node.id).then(function(response){
                console.log(response.data);
                node.fields = response.data.result.fields;
                catalog.newNode = JSON.parse(JSON.stringify(node));
                catalog.activeNode = node;
                catalog.isFormShow = true;
            });
        };

        catalog.cancel = function () {
            catalog.isFormShow = false;
            catalog.activeNode = [ ];
            catalog.newNode = [ ];
        };

        catalog.saveNode = function () {
            catalog.isFormShow = false;
            catalog.activeNode.name = catalog.newNode.name;
            catalog.activeNode.fields = catalog.newNode.fields;
            console.log(catalog.activeNode.name);
            console.log(catalog.activeNode.fields);
        }
    }]);

    app.directive('nodeTree', function(){
        return {
            restrict: 'A',
            templateUrl: 'node.html'
        };
    });
})();