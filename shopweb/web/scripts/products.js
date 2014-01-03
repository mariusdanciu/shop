(function() {
  $(function() {

   
    $(".item_list ul li div").each(function( index ) {
      var me = $(this); 
      
      $(this).mouseenter(function(){
        me.find('.info_tag').css( 'background', 'rgba(100, 100, 100, .7)' );
      });
   
      $(this).mouseleave(function(){
        me.find('.info_tag').css( 'background', 'rgba(100, 100, 100, .3)' );
      });
      
    });

    cart.loadView();


  });
  
})();