(function() {
	$(function() {
		refreshList();

		$("#sortSelect").chosen({
			"disable_search" : true
		});
		
		$('#sortSelect').on('change', function(evt, params) {
			var cat = $.url().param("cat");
			$("#item_list").load(normUrl("/products", $(this).val()), function() {
			  refreshList();
			});
		})
		
	});

	var normUrl = function(url, sort) {
		var cat = $.url().param("cat");
		var search = $.url().param("search");

		if (cat === undefined) {
			url += "?search=" + search;
		} else {
			url += "?cat=" + cat;
		}
		url += "&sort=" + sort;
		return url;
	};

	var refreshList = function() {
		$(".item_box").each(function(index) {
			var me = $(this);

			me.mouseenter(function() {
				me.css({
					'cursor' : 'pointer'
				});
				me.find('.info_tag').css({
					'background' : 'rgba(0, 0, 0, 1)'
				});
				me.find('.info_tag *').css({
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
				me.find('.info_tag *').css({
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
	}

})();