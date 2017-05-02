(function(){
    var app = angular.module('catalog', [ ]);

    app.controller('nodeController', [ '$http', function($http){
        var catalog = this;
        catalog.nodes = [ ];
        $http.get('api/gettree/0').then(function(response){
            console.log(response.data);
            catalog.nodes = response.data.result;
        });

        this.expand = function(node){
            node.isExpanded = !node.isExpanded;
            if (node.isExpanded && !node.nodes) {
                node.nodes = [ ];
                $http.get('api/gettree/' + node.id).then(function(response){
                    console.log(response.data);
                    node.nodes = response.data.result;
                });
            }
        };
    }]);

    app.directive('nodeTree', function(){
        return {
            restrict: 'A',
            templateUrl: 'node.html'
        };
    });
})();