(function() {
  $(function() {

    $("#cart_symbol").click(function(event) {
      $('#cart_popup').show();
      event.stopPropagation();
    });
    
    $("#cart_popup").click(function(event) {
      event.stopPropagation();
    });

    $(document).click(function() {
      $('#cart_popup').hide();
    });
    
    $(document).keyup(function(e) {
     if (e.keyCode == 27) {
       $('#cart_popup').hide();
     }
    });
   
    cart = {

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
      },
   
      removeItem : function(id) {
        var c = $.cookie("cart");
        if (c) {
          var cart = $.parseJSON(c);
          var a = cart.items;
          var na = [];
          for (i in a) {
            if (a[i].id != id) {
              console.log(a[i].id + " " + id);
              na.push(a[i])
            }
          }
          cart.items = na;
          $.cookie("cart", JSON.stringify(cart));
          window.cart.loadView();
        } 
      },
   
      loadView : function() {
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
        }).fail(function(msg, f) {
            alert(f);
        });
      }

   } 
   
  });
  
})();