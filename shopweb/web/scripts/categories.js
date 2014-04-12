(function() {
	$(function() {

		$(".cat_box").each(function(index) {
			var me = $(this);

			me.mouseenter(function() {
				me.css({
					'cursor' : 'pointer'
				});
				me.find('.info_tag').css({
					background : 'rgba(0, 0, 0, 1)'
				});
				me.find('.info_tag div').css({
					'color' : '#ffffff'
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
			});
			
			me.click(function(event) {
				var pid = me.attr("id");
				window.location.href = "/products?cat=" + pid;
				event.stopPropagation();
			});

		});

	});

})();