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
									$("#notice_connect_e").show().delay(5000).fadeOut("slow");
								} else {
									refreshList();
								}
							});
				})
		
		Dropzone.autoDiscover = false;
		
		$( ".close_dialog" ).bind( "click", function(event) {
			closeDialog();
		});
		
		var uploadZone = new Dropzone("#upload_dialog", { 
		   uploadMultiple: true,
		   autoProcessQueue:false,
           parallelUploads: 100,
           maxFiles: 100,
           addRemoveLinks: true
		});
 
        $("#create_product").click(function(event) {
           alert("save");
           event.stopPropagation();
           event.preventDefault();
        });
        
		$("#itemadd").click(function(event) {
	        uploadZone.removeAllFiles(true);
	        $.blockUI({ 
	        	message: $("#product_create_dialog"),
	            css: { 
	                top:  '120px', 
	                left: ($(window).width() - 1000) /2 + 'px', 
	                width: '1050px',
	                border: 'none',
	                cursor: null
	            },
		        overlayCSS:  {
					cursor: null,
					backgroundColor: '#dddddd'
				}
	        });
	    });				
	    
	    
	    $("#textzone").jqte({
	      source: false
	    }); 
	    
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

			me.find('.info_tag_cart').click(function(event) {
				var pid = me.attr("id");
				cart.addItem(pid);
				cart.showCart();
				event.stopPropagation();
			});
    
    		var pid = me.attr("id");
			if (pid !== undefined) {
			  me.click(function(event) {
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
						                top:  '150px', 
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
						        
						        
						        $( ".close_dialog" ).bind( "click", function(event) {
						        	closeDialog();
						        });
							}
						});
				event.stopPropagation();
			  });
			}
		});
	}
				
})();