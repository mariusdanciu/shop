(function() {

	'use strict';

	// iPad and iPod detection
	var isiPad = function() {
		return (navigator.platform.indexOf("iPad") != -1);
	};

	var isiPhone = function() {
		return ((navigator.platform.indexOf("iPhone") != -1) || (navigator.platform
				.indexOf("iPod") != -1));
	};

	// Owl Carousel
	var owlCrouselFeatureSlide = function() {
		var owl = $('.owl-carousel1');
		owl.owlCarousel({
			items : 1,
			loop : true,
			margin : 0,
			responsiveClass : true,
			nav : true,
			dots : true,
			smartSpeed : 500,
			navText : [ "<i class='icon-chevron-left owl-direction'></i>",
					"<i class='icon-chevron-right owl-direction'></i>" ]
		});

		$('.owl-carousel2')
				.owlCarousel(
						{
							loop : true,
							margin : 10,
							nav : true,
							dots : true,
							responsive : {
								0 : {
									items : 1
								},
								600 : {
									items : 3
								},
								1000 : {
									items : 3
								}
							},
							navText : [
									"<i class='icon-chevron-left owl-direction'></i>",
									"<i class='icon-chevron-right owl-direction'></i>" ]
						})
	};

	// Owl Carousel

	// Parallax
	var parallax = function() {
		$(window).stellar();
	};

	var goToTop = function() {

		$('.js-gotop').on('click', function(event) {

			event.preventDefault();

			$('html, body').animate({
				scrollTop : $('html').offset().top
			}, 500);

			return false;
		});

	};

	// Offcanvas and cloning of the main menu
	var offcanvas = function() {

		var $clone = $('.nav').clone();
		$clone.attr({
			'id' : 'offcanvas-menu'
		});
		$clone.find('> ul').attr({
			'class' : '',
			'id' : ''
		});

		$('.box-wrap').prepend($clone);

		// click the burger
		$('.js-fh5co-nav-toggle').on('click', function() {

			if ($('body').hasClass('fh5co-offcanvas')) {
				$('body').removeClass('fh5co-offcanvas');
			} else {
				$('body').addClass('fh5co-offcanvas');
			}
			// $('body').toggleClass('fh5co-offcanvas');

		});

		$('#offcanvas-menu').css('height', $(window).height());

		$(window).resize(function() {
			var w = $(window);

			$('#offcanvas-menu').css('height', w.height());

			if (w.width() > 769) {
				if ($('body').hasClass('fh5co-offcanvas')) {
					$('body').removeClass('fh5co-offcanvas');
				}
			}

		});

	}

	// Window Scroll
	var windowScroll = function() {
		var lastScrollTop = 0;

		$(window)
				.scroll(
						function(event) {

							var header = $('#fh5co-header'), scrlTop = $(this)
									.scrollTop();

							if (scrlTop > 500 && scrlTop <= 2000) {
								header
										.addClass('navbar-fixed-top fh5co-animated slideInDown');
							} else if (scrlTop <= 500) {
								if (header.hasClass('navbar-fixed-top')) {
									header
											.addClass('navbar-fixed-top fh5co-animated slideOutUp');
									setTimeout(
											function() {
												header
														.removeClass('navbar-fixed-top fh5co-animated slideInDown slideOutUp');
											}, 100);
								}
							}

						});
	};

	// Animations

	var contentWayPoint = function() {
		var i = 0;
		$('.animate-box').waypoint(function(direction) {

			if (direction === 'down' && !$(this.element).hasClass('animated')) {

				i++;

				$(this.element).addClass('item-animate');
				setTimeout(function() {

					$('body .animate-box.item-animate').each(function(k) {
						var el = $(this);
						setTimeout(function() {
							var effect = el.data('animate-effect');
							if (effect === 'fadeIn') {
								el.addClass('fadeIn animated');
							} else {
								el.addClass('fadeInUp animated');
							}

							el.removeClass('item-animate');
						}, k * 200, 'easeInOutExpo');
					});

				}, 100);

			}

		}, {
			offset : '85%'
		});
	};

	// Document on load.
	$(function() {

		// Animations
		owlCrouselFeatureSlide();
		contentWayPoint();
		parallax();

		$("#login").click(function(e) {
			var cls = $("#login span").attr("class")
			if (cls === "icon-exit") {
				return true;
			} else {
				$("#login-box").toggle();
				$("#username").focus();
				e.preventDefault();
				return false;
			}
		});

		$("#forgot-pass-btn").click(function(e) {
			window.user.forgotPassword("#login_form");
			e.preventDefault();
			return false;
		});

		$("#login-btn").click(function(e) {
			window.user.login("#login_form");
			e.preventDefault();
			return false;
		});

		$("#login_form").keydown(function(event) {
			if (event.keyCode == 13) {
				window.user.login("#login_form");
				return false;
			}
		});

		$("#search-icon").click(function(e) {
			$("#search-box").toggle();
			$("#search").focus();
			e.preventDefault();
			return false;
		});

		$("#search").keydown(
				function(event) {
					if (event.keyCode == 13) {
						var s = $("#search").val();

						var url = window.common.normUrl("/products/find", $.url().param(
								"sort"), s);

						console.log(url);
						window.location.href = url;

						event.preventDefault();
						return false;
					}
				});

	});

    console.log("Attach");

	$("#cart_link").click(function(e){
	   var cartItems = {
	     'items': cart.items()
	   }

	   var enc = $.base64.encode(JSON.stringify(cartItems));
	   var href = $(this).attr("href") + "?cart=" + encodeURIComponent(enc);
       $(this).attr("href", href);

       return true;
	});

}());

var user = {
	login : function(frmId) {
		var creds = $.base64.encode($(frmId + " #username").val() + ":"
				+ $(frmId + " #password").val());

		console.log(creds);

		$.ajax({
			url : $(frmId).attr('action'),
			type : "GET",
			cache : false,
			timeout : 3000,
			headers : {
				'Authorization' : "Basic " + creds
			},
			statusCode : {
				200 : function() {
					window.location.href = "/";
				},
				406 : function(m) {
					console.log(m);
					window.common.showNotice(m.responseText);
				}
			}
		});
	},

};

var common = {

	normUrl : function(url, sort, search) {
		var params = [];

		if (sort !== undefined && sort !== null) {
			params.push("sort=" + sort);
		}
		if (search !== undefined && search !== null) {
			params.push("search=" + search);
		}

		var q = ""
		for ( var i in params) {
			if (i > 0) {
				q += "&";
			}
			q += params[i];
		}

		return url + "?" + q;
	},

	showNotice : function(text, parentDiv) {
	    if (parentDiv !== undefined) {
 	        parentDiv.append("<div class='notice_overlay'><span>" + text + "</span></div>");
	        setTimeout(function() {
              parentDiv.find('.notice_overlay').remove();
            }, 3000);
	    } else {
		  $("#notice_i").html("<span>" + text + "</span>");
		  $("#notice_i").show().delay(5000).fadeOut("slow");
		}
	},

	showFormErrors : function(errors, title) {
	    $(".form_errors ul").html("");
	    $(".form_errors ul").append("<li> <b>" + title + "</b> </li>");

		$.each(errors, function() {
			$(".form_errors ul").append("<li>" + this.error + "</li>");
		});
	},

	save : function(formId, successFunc) {
		$(formId).each(function() {
			var frm = this;
			var formData = new FormData(frm);

			$(formId + ' label').css("color", "#555555").removeAttr("title");

			$.ajax({
				url : $(frm).attr('action'),
				type : "POST",
				cache : false,
				contentType : false,
				processData : false,
				timeout : 10000,
				data : formData,
				statusCode : {
					200 : function(data) {
						successFunc(data);
					},
					201 : function(data) {
						successFunc(data);
					},
					403 : function(msg) {
						var data = JSON.parse(msg.responseText);
						if (data.errors) {
							window.common.showFormErrors(data.errors, "Produsul nu a fost salvat.");
						}
						return false;
					}
				}
			});
		});
	}

};