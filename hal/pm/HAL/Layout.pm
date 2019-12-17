#-*-perl-*-
package HAL::Layout;
require Exporter;
@ISA=qw(Exporter);
@EXPORT = qw(htmlPage htmlPageWithMenu outputFrontPage);
use strict;
use warnings;
use POSIX;
use utf8;
use HAL::Session;

sub htmlPage($$;$) {
    my ($title, $body, $opt) = @_;
    
    my $headers = '';
    if ($opt) {
	if ($opt->{feed}) {
	    $headers .= qq'\n  <link rel="alternate" type="application/rss+xml" '.
		qq'title="RSS feed" href="$opt->{feed}/feed.rss" />';
	}
    }

    my $now = $$;    

    $headers .= qq'\n  <META HTTP-EQUIV="Refresh" CONTENT="30">' if $opt->{autorefresh};    

    if (my $js = $opt->{js}) {      
      my @js = ref $js eq 'ARRAY' 
         ? @$js
         : ($js);
         
      for my $j (@js) {
        $headers .= qq'\n  <script type="text/javascript" src="/hal-static/$j?now=$now"></script>';
      }
    }
    $headers .= qq'\n  <script type="text/javascript" src="/hal-static/typeahead.js?now=$now"></script>';
    $headers .= qq'\n  <script type="text/javascript" src="/hal-static/sorttable.js"></script>';

    my $onload = '';
    if ($opt->{onload}) {
	$onload = qq' onload="$opt->{onload}"';
    }
    
    $title .= " @ ".scalar strftime("%a, %d  %b  %Y  %H:%M:%S  %Z", localtime(time));
    
    return qq'<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html
        PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
         "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en-US" xml:lang="en-US">
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
  <title>$title</title>
  <style type="text/css">\@import "/hal-static/style.css";</style>
  <style type="text/css">\@import "/hal-static/Chart.css";</style>
  <link rel="shortcut icon" href="/hal-static/hal-100.png"/>$headers
</head><body$onload>
$body
</body></html>';
}

sub htmlPageWithMenu {
    my ($opt, $items, $content, $js) = @_;
    
    my $menu = '';
    for my $item (@$items) {
	if ($item->{current}) {
	    $opt->{title} ||= $item->{title};
	    $menu .= qq'                    <li><span>$item->{title}</span></li>\n';
	} else {
	    $menu .= qq'                    <li><a href="$item->{link}">$item->{title}</a></li>\n';
	}
    }
    $opt->{title} ||= ''; # No title?

    my ($name) = isLoggedIn ? split(/ /, getSession->{name}) : 'Dave';

    my $logo = qq'<img src="/hal-static/hal-100.png" alt="HAL 900" title="Just what do you think you\'re doing, $name?"/>';
    $logo = qq'<a href="/hal/" id="logo" title="Back to the front page">$logo</a>' unless $opt->{dontLinkLogo};
    
    my $feed = '';
    if ($opt->{feed}) {
	my $webPage = qq'\n  <li class="feeditem"><a href="$opt->{feed}/" title="See the newsfeed about this page">Web page</a></li>';
	$webPage = '' if $opt->{noFeedPage};
	$feed = qq'<div id="feeds"><span class="tpop"><img class="intp" src="/hal/news-feeds-100x16.png" width="100" height="16"/><span class="apop">
<ul>$webPage
  <li class="feeditem"><a href="$opt->{feed}/feed.rss" title="Subscribe to the newsfeed using RSS">RSS feed</a></li>
</ul>
        </span></span></div>
';
    }

    if (isLoggedIn()) {
	my $name = getSession->{username};
	$feed .= qq'<div id="logout"><strong>Bruger: </strong><a href="/hal/account/">$name</a> - [<a href="/hal/account/logout">Log af</a>]</div>';
    }

    my $title;
    my $titleHtml = '';
    my $titleClass = '';
    
    if (ref $opt->{title}) {
	my $t = $opt->{title};
	
	$title = $t->{title};           
	$titleHtml = $t->{html};
	$titleClass = qq' class="$t->{class}"' if $t->{class};	
    } else {
	$titleHtml = qq'<h1 id="title">$opt->{title}</h1>';
	$title = $opt->{title};
    }
    
    my $body = qq'
<div id="head-div">
  <div id="logo-div">$logo</div>
  <div id="title-div"$titleClass>$titleHtml</div>
  <div id="nav-menu"><ul>$menu</ul></div>
  $feed
</div>
<div id="main"><div id="main-content">$content</div></div>
';

    return htmlPage($title, $body, $opt); 
}

sub outputFrontPage {
    my ($cur, $title, $body, $feed, $js) = @_;
    
    my @items = (
	{
	    link=>"/hal/",
	    name=>'index',
	    title=>'HAL:900',
	},
	);

    if (isLoggedIn) {
	push @items, (
	    {
		link=>"/hal/account/",
		name=>'account',
		title=>'Bruger Oversigt',
	    },
            {
                link=>"/hal/status/member-count",
                name=>'status',
                title=>'Status',
            },
            {
                link=>"/hal/lokaler/eval",
                name=>'lokaler',
                title=>'Lokaler',
            },
	);

	if (isAdmin) {
	    push @items, (
		{
		    link=>"/hal/admin/",
		    name=>'admin',
		    title=>'Admin',
		}
	    );
	}

    } else {
	push @items, (
	{
	    link=>"/hal/new",
	    name=>'new',
	    title=>'Ny bruger',
	},
	{
	    link=>"/hal/login",
	    name=>'login',
	    title=>'Login',
	},
	);
    }
    
    for my $i (@items) {
	$i->{current}=1 if $i->{name} eq $cur;
    }
    
    return {
	opt=>{
	    title=>$title,
	    feed=>$feed,
	    dontLinkLogo=>$cur eq 'index',
	    noFeedPage=>$cur eq 'news',
            js=>$js,
	},
	body=>$body,
	items=>\@items,         
    }
} 



1;
