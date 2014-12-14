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
          $("#item_list").load(normUrl("/products", $(this).val()),
              function(response, status, xhr) {
                if (status == "error") {
                  $("#notice_connect_e").show().delay(5000).fadeOut("slow");
                } else {
                  refreshList();
                }
              });
        })

    $(".close_dialog").bind("click", function(event) {
      closeDialog();
    });

    $('#create_product_tab').tabify();

    $("#create_product").click(function(event) {
      saveProduct("#upload_form");
      event.stopPropagation();
      event.preventDefault();
    });
    
    $("#create_description").jqte({
      br: false,
      b: false,
      source: false
    });

    $("#add_prop")
        .click(
            function(event) {
              var div = $("<div class='row'></div>");
              div.append("<input type='text' name='pkey'/><input type='text' name='pval'/>");
              var remove = $("<img class='clickable' src='/static/images/minus.png'/>");
              remove.click(function(e) {
                div.remove();
              });
              div.append(remove);

              $("#prop_fields").append(div);
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

  var reloadProducts = function() {
    $("#item_list").load(normUrl("/products", $('#sortSelect').val()),
        function(response, status, xhr) {
          if (status == "error") {
            $("#notice_connect_e").show().delay(5000).fadeOut("slow");
          } else {
            refreshList();
          }
        });
  };

  var closeDialog = function() {
    $.unblockUI();
    setTimeout(function() {
      $("#product_dialog").empty();
      $(".zoomContainer").remove();
    }, 400);
  }

  var deleteProduct = function(id) {
    $.ajax({
      url : "/product/delete/" + id,
      type : "DELETE"
    }).success(reloadProducts()).fail(function(msg, f) {
      $("#notice_connect_e").show().delay(5000).fadeOut("slow");
    });
  }
  
  var saveProduct = function(formId) {
    $(formId).each(function() {
      var frm = this;
      var formData = new FormData(frm);
      
      $(formId + ' label').css("color", "#000000").removeAttr("title");
      $.ajax({
        url : $(frm).attr('action'),
        type : "POST",
        cache : false,
        contentType : false,
        processData : false,
        data : formData,
        statusCode: {
          201: function() {
            closeDialog();
            reloadProducts();
          }
        }
      }).fail(function(msg, f) {
        $("#notice_connect_e").show().delay(5000).fadeOut("slow");
      })
    });
  };
  
  var refreshList = function() {
    $(".item_box").each(function(index) {
      var me = $(this);

      me.find('.edit_tag_close').click(function(event) {
        var pid = me.attr("id");
        deleteProduct(pid);
        event.preventDefault();
        event.stopPropagation();
      });

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
          $("#product_dialog").load(loc, function(response, status, xhr) {
            if (status == "error") {
              $("#notice_connect_e").show().delay(5000).fadeOut("slow");
            } else {

              $("#sel_img").elevateZoom({
                gallery : 'detail_box',
                cursor : 'pointer',
                galleryActiveClass : 'active',
                imageCrossfade : true,
                loadingIcon : '/images/ajax-loader.gif',
                scrollZoom : true,
                borderSize : 1
              });

              $('#add_to_cart').click(function(event) {
                closeDialog();
                cart.addItem(pid);
                cart.showCart();
                event.stopPropagation();
              });

              $("#save_product").click(function(event) {
                saveProduct("#edit_form", "/product/update");
                event.stopPropagation();
                event.preventDefault();
              });
              
              $.blockUI({
                message : $("#product_dialog"),
                css : {
                  top : '100px',
                  left : ($(window).width() - 1000) / 2 + 'px',
                  width : '1000px',
                  border : 'none',
                  cursor : null
                },
                overlayCSS : {
                  cursor : null,
                  backgroundColor : '#dddddd'
                }
              });

              $('#product_details_tab').tabify();
              $('#edit_product_tab').tabify();
              
              $("#edit_description").jqte({
                br: false,
                source: false
              });

              $(".close_dialog").bind("click", function(event) {
                closeDialog();
              });
            }
          });
          event.stopPropagation();
        });
      } else {
        $("#itemadd").click(function(event) {
          $.blockUI({
            message : $("#product_create_dialog"),
            css : {
              top : '70px',
              left : ($(window).width() - 800) / 2 + 'px',
              width : '800px',
              border : 'none',
              cursor : null
            },
            overlayCSS : {
              cursor : null,
              backgroundColor : '#dddddd'
            }
          });
        });
      }
    });
  }

})();