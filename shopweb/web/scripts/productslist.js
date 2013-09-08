(function() {
  $(function() {
   
    $(".ajax_block_product").each(function( index ) {
      var me = $(this); 
      
      $(this).mouseenter(function(){
        me.find('.price_tag').css( 'background', 'rgba(100, 100, 100, .7)' );
      });
   
      $(this).mouseleave(function(){
        me.find('.price_tag').css( 'background', 'rgba(100, 100, 100, .3)' );
      });
    });
  });
  
})();