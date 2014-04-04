(function() {
	$(function() {
		$(".item_box").each(function(index) {
			var me = $(this);
			
			me.mouseenter(function() {
				me.css({
					'cursor' : 'pointer'
				});
				me.find('.info_tag').css({
					'background' : 'rgba(0, 0, 0, 1)'
				});
				me.find('.info_tag div').css({
					'color' : '#ffffff'
				});
				me.find('.info_tag_cart').css({
					'display' : 'inline'
				});
				
			});

			me.mouseleave(function() {
				me.css({
					'cursor' : 'hand'
				});
				me.find('.info_tag').css({
					'background' : 'rgba(255, 255, 255, .5)'
				});
				me.find('.info_tag div').css({
					'color' : '#000000'
				});
				me.find('.info_tag_cart').css({
					'display' : 'none'
				});
			});
			
			me.find('.info_tag_cart').click(function(event) {
				var pid = me.attr("id");
				cart.addItem(pid);
				cart.showCart();
				event.stopPropagation();
			});
			
			me.click(function(event) {
				var pid = me.attr("id");
				window.location.href = "/product?pid=" + pid;
				event.stopPropagation();
			});
			
		});
		
	});

})();