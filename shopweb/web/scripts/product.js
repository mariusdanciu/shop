(function() {
  $(function() {

    $("#add_to_cart").click(function(event) {
      var pid = $.url().param("pid");
      cart.addItem(pid);
      cart.loadView();
      $('#cart_popup').show();
      event.stopPropagation();
    });
    
    cart.loadView();
    
  });
  
})();