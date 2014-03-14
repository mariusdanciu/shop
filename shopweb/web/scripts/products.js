(function() {
	$(function() {
		$(".item_box").each(function(index) {
			var me = $(this);

			$(this).mouseenter(function() {
				me.find('.info_tag').css({
					background : 'rgba(0, 0, 0, 1)'
				});
				me.find('.info_tag div').css({
					'color' : '#ffffff'
				});
				me.find('.info_tag_icons').css({
					visibility : 'visible'
				});
			});

			$(this).mouseleave(function() {
				me.find('.info_tag').css({
					'background' : 'rgba(255, 255, 255, .5)'
				});
				me.find('.info_tag div').css({
					'color' : '#000000'
				});
				me.find('.info_tag_icons').css({
					visibility : 'hidden'
				});
			});

		});

	});

})();