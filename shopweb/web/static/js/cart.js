(function() {
	$(function() {
		$("#cart_num").text(cart.numItems());

		$('.cart_delete').each(function(i, obj) {
			$(obj).click(function(e) {
				var id = $(this).attr("id");
				cart.removeItem(i);

				var cartItems = {
                   'items': cart.items()
                }

                var enc = $.base64.encode(JSON.stringify(cartItems));
                window.location = "/cart?cart=" + encodeURIComponent(enc);

				e.stopPropagation();
				return false;
			});
		});

		
		$("#buy").click(function(e) {
			window.cart.buy(function() {
				window.cart.clear();
				window.location = "/order_done";
			});
			e.preventDefault();
			e.stopPropagation();
			return false;
		});
		
	});
})();

var cart = {
	buy : function(okFunc) {

		var form = "#order_form";

		var formObj = {};
		var obj = $(form).serializeArray();

		$.each(obj, function() {
			formObj[this.name] = this.value;
		});

		var items = cart.items();
		formObj["items"] = [];
		for (e in items) {
			formObj["items"].push({
				name : items[e].id,
				value : items[e].count
			})
		}

		$(form + ' label').css("color", "#555555").removeAttr("title");

		$.ajax({
			url : "/order",
			contentType : 'application/json; charset=UTF-8',
			data : JSON.stringify(formObj),
			cache : false,
			timeout : 3000,
			type : 'POST',
			statusCode : {
				403 : function(msg) {
					var data = JSON.parse(msg.responseText);
					if (data.errors) {
						window.common.showFormErrors(data.errors);
					}
				},

				200 : function(data) {
					okFunc();
				}
			}
		});
		return false;

	},

	clear : function() {
	    window.localStorage.removeItem("cart")
	},

	computeTotal : function(add) {
		var total = 0;

		$(".cart_item").each(function() {
			var num = $(this).find("input").val();
			var price = $(this).find(".cart_price").text();
			total += num * price;
		});
		if (add) {
			total += add;
		}

		return parseFloat(total).toFixed(2);
	},

	items : function() {
		var c = window.localStorage.getItem("cart");
		if (c) {
			return $.parseJSON(c).items;
		}
		return [];
	},

	numItems : function() {
		var num = 0;
		var it = this.items();
		for (i in it) {
			num = num + it[i].count;
		}
		return num;
	},

	addItem : function(id) {
		var c = window.localStorage.getItem("cart");
		console.log(c);
		if (c) {
			var cart = $.parseJSON(c);
			var a = cart.items;
			var found = undefined;
			for (i in a) {
				if (a[i].id === id) {
					found = a[i];
				}
			}

			if (!found) {
				cart.items.push({
					id : id,
					count : 1
				});
			} else {
				found.count = found.count + 1;
			}
			window.localStorage.setItem("cart", JSON.stringify(cart));
		} else {
			window.localStorage.setItem("cart", JSON.stringify({
				items : [ {
					id : id,
					count : 1
				} ]
			}));
		}
		$("#cart_num").text(this.numItems());
	},

	setItemCount : function(pos, count) {
		var c = window.localStorage.getItem("cart");
		if (c) {
			if (count === "")
				count = 1;

			var cart = $.parseJSON(c);
			var a = cart.items;
			for (i in a) {
				if (i == pos) {
					a[i].count = parseInt(count);
				}
			}
			window.localStorage.setItem("cart", JSON.stringify(cart));
			$("#cart_num").text(this.numItems());
		}
	},

	removeItem : function(pos) {
		var c = window.localStorage.getItem("cart");
		if (c) {
			var cart = $.parseJSON(c);
			var a = cart.items;
			var na = [];
			for (i in a) {
				if (i != pos) {
					na.push(a[i])
				}
			}
			cart.items = na;
			window.localStorage.setItem("cart", JSON.stringify(cart));
			$("#cart_num").text(this.numItems());
		}
	}

}