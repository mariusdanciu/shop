(function() {
	$(function() {
		
		$.blockUI.defaults.baseZ = 90;
		
		refreshList();

		$("#sortSelect").chosen({
			"disable_search" : true
		});

		$('#sortSelect').on(
				'change',
				function(evt, params) {
					$("#item_list").load(
							normUrl("/products", $(this).val()),
							function(response, status, xhr) {
								if (status == "error") {
									$("#notice_connect_e").show().delay(5000)
											.fadeOut("slow");
								} else {
									refreshList();
								}
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

	var closeDialog = function() {
		$.unblockUI();
    	setTimeout(function(){
    		$("#product_dialog").empty();
    		$(".zoomContainer").remove();
    	}, 400); 
	}
	
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
				me.find('.info_tag span, .info_tag div').css({
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
				me.find('.info_tag span, .info_tag div').css({
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
				var loc = "/productquickview?pid=" + pid;
				$("#product_dialog").load(loc,
						function(response, status, xhr) {
							if (status == "error") {
								$("#notice_connect_e").show().delay(5000)
										.fadeOut("slow");
							} else {

								$("#sel_img").elevateZoom({
									gallery : 'detail_box',
									cursor : 'pointer',
									galleryActiveClass : 'active',
									imageCrossfade : true,
									loadingIcon : '/images/ajax-loader.gif',
									scrollZoom : true,
									borderSize: 1
								});
								
								$('#add_to_cart').click(function(event) {
									closeDialog();
									cart.addItem(pid);
									cart.showCart();
									event.stopPropagation();
								});
								
						        $.blockUI({ 
						        	message: $("#product_dialog"),
						            css: { 
						                top:  ($(window).height() - 650) /2 + 'px', 
						                left: ($(window).width() - 1000) /2 + 'px', 
						                width: '1000px',
						                border: 'none',
						                cursor: null
						            },
						            overlayCSS:  {
										cursor: null,
										backgroundColor: '#dddddd'
									}
						        }); 
						        
						        
						        $( ".blockOverlay, #close_dialog" ).bind( "click", function(event) {
						        	closeDialog();
						        });
							}
						});
				
				
				
				// window.location.href = "/product?pid=" + pid;
				event.stopPropagation();
			});

		});
	}
	
})();