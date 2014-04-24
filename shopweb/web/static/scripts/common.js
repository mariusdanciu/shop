(function() {
  $(function() {

    $("#cart_symbol").click(function(event) {
      cart.showCart();
      event.stopPropagation();
    });
    
    $("#cart_popup").click(function(event) {
      event.stopPropagation();
    });

    $(document).click(function() {
      cart.hideCart();
    });
    
    $(document).keyup(function(e) {
     if (e.keyCode == 27) {
        cart.hideCart();
     }
    });
    
    $("#buy_step1").click(function(event) {
      cart.showStep1Links();
      event.stopPropagation();
    });
    
    
    $('#buy_final').click(function(event) {
      var obj = $("#order_form").serializeArray();
      var items = cart.items();
      for (e in items) {
        obj.push({name : "item."+items[e].id, value: items[e].count})
      }
      
      cart.cleanFormMessages();
      $.ajax({
		  url: "/order",
		  data: obj
        });
    });
    
    
    $("#search_text").keypress(function(e) {
        if(e.which == 13) {
           var text = $("#search_text").val();
           window.location.href = '/products?search='+text;
        }
    });
    
    cart = {

      orderDone: function(msg){
        $('#cart_notice').show();
        $('#cart_notice').text(msg);
        window.cart.clear();
        $('#cart_empty').hide();
      },

	  cleanFormMessages : function() {
	    $('#order_form label').css("color", "#000000").removeAttr("title");
	  },
	  
	  clear : function() {
	    $.cookie("cart", JSON.stringify({ items : [] }));
	    window.cart.loadView();
	  },

      computeTotal: function(){
        var total = 0;
        
        $(".cart_item").each(function() {
          var num = $(this).find("input").val();
          var price = $(this).find(".cart_price").text();
          total += num * price;
        });
        
        $("#total").text(parseFloat(total).toFixed(2) + " RON");
      },

      items : function(){
        var c = $.cookie("cart"); 
        if (c) {
           return $.parseJSON(c).items;
        }
        return [];
      },
      
      addItem : function(id) {
        var c = $.cookie("cart");
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
             cart.items.push({id: id, count: 1});
           } else {
             found.count = found.count + 1;
           }
           $.cookie("cart", JSON.stringify(cart));
        } else {
          $.cookie("cart", JSON.stringify({ items : [{id: id, count: 1}] }));
        }
        window.cart.loadView();
      },
   
      setItemCount : function(id, count) {
        var c = $.cookie("cart");
        if (c) {
          if (count === "") count = 1;
          
          var cart = $.parseJSON(c);
          var a = cart.items;
          for (i in a) {
            if (a[i].id === id) {
              a[i].count = parseInt(count);
            }
          }
          $.cookie("cart", JSON.stringify(cart));
          window.cart.computeTotal();
          $(this).focus();
        }
      },
   
      removeItem : function(id) {
        var c = $.cookie("cart");
        if (c) {
          var cart = $.parseJSON(c);
          var a = cart.items;
          var na = [];
          for (i in a) {
            if (a[i].id != id) {
              na.push(a[i])
            }
          }
          cart.items = na;
          $.cookie("cart", JSON.stringify(cart));
          window.cart.loadView();
        } 
      },
   
      loadView : function() {
        if (window.cart.items().length === 0) {
            $('#order').hide();
            $('#cart_content').hide();
            $('#cart_footer').hide();
            $('#cart_empty').show();
        } else {
          $('#cart_empty').hide();

          $.ajax({
	        url: "/getcart",
		    dataType: "json",
		    context: $("#cart_content")
          }).done(function(data) {
            $( this ).empty();
            var ul = document.createElement("ul");
            for (item in data) {
              var li = document.createElement("li");
              $(li).append(data[item]);
            	$(ul).append(li);
            }
            $( this ).append(ul);
            window.cart.computeTotal();
          
           $('#cart_content').show();
           $('#cart_footer').show();
         
          
        }).fail(function(msg, f) {
            alert(f);
        });
        }
      },
      
      showStep1Links: function() {
        $('#order').show();
        $('#buy_step1').hide();
        $('#buy_final').show();
      },
      
      showCart: function() {
        $('#order').hide();
        $('#cart_notice').hide();
        $('#buy_final').hide();
        $('#buy_step1').show();
        window.cart.loadView();
        $('#cart_popup').show();
        
      },
      
      hideCart: function() {
        $('#cart_popup').hide();
        $('#cart_notice').hide();
        $('#order').hide();
        $('#buy_final').hide();
        $('#buy_step1').show();
      }

   } 
   
   cart.loadView();
  });
  
})();