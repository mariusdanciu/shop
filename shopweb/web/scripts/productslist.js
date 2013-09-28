(function() {
  $(function() {

   
    $(".products_list ul li div").each(function( index ) {
      var me = $(this); 
      
      $(this).mouseenter(function(){
        me.find('.price_tag').css( 'background', 'rgba(100, 100, 100, .7)' );
      });
   
      $(this).mouseleave(function(){
        me.find('.price_tag').css( 'background', 'rgba(100, 100, 100, .3)' );
      });
      
    });

    $("#cart_symbol").click(function(event) {
      $('#cart_popup').show();
      event.stopPropagation();
    });
    
    $("#cart_popup").click(function(event) {
      event.stopPropagation();
    });

    $(document).click(function() {
      $('#cart_popup').hide();
    });
    
    $(document).keyup(function(e) {
     if (e.keyCode == 27) {
       $('#cart_popup').hide();
     }
    });
    
  });
  
})();