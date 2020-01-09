#-*-perl-*-
package HAL::Page::Lokaler;
use strict;
use warnings;
use utf8;

use Data::Dumper;
use HTML::Entities;
use Digest::SHA qw(sha1_hex);

use HAL;
use HAL::Pages;
use HAL::Session;
use HAL::Util;
use HAL::Email;
use HAL::Layout;

sub table {
    my @table = @_;

    my $head = shift @table;
    my $html = '<tr>'.join("", map {"<th>$_</th>"} @$head)."</tr>\n";
    
    my $i = 0;
    for my $row (@table) {
	my $class = ($i++ & 1) ? 'class="odd"' : 'class="even"';
	$html .= "<tr $class>".join("", map {"<td>$_</td>"} @$row)."</tr>\n";
    }

    return qq'<table>$html</table>\n';
}

sub adminPage { 
  my ($r,$q,$p) = @_;
  
  if ($p->{add}) {
      db->sql("insert into space_option (title, uri, description) values (?,?,?)",
	      $p->{title}, $p->{uri}, $p->{desc});
      delete $p->{title};
      delete $p->{uri};
      delete $p->{desc}; 
  }

  my @table = [ qw'id Title URI Description Remove' ];
  my $oq = db->sql("select id, title, uri, description from space_option order by id");
  while (my ($id, $title, $uri, $description) = $oq->fetchrow_array) {
      if ($p->{"del$id"}) {
	  db->sql("delete from space_option where id=?", $id);
	  next;
      }
      
      if ($p->{store} && (
	      $p->{"title$id"} ne $title or
	      $p->{"uri$id"} ne $uri or
	      $p->{"desc$id"} ne $description
	  )) {

	  db->sql("update space_option set title=?, uri=?, description=? where id=?",
		  $p->{"title$id"}, $p->{"uri$id"}, $p->{"desc$id"}, $id);
	  
	  $title = $p->{"title$id"};
	  $uri = $p->{"uri$id"};
	  $description = $p->{"desc$id"};
      }
      
      push @table, [
	  $id,
	  qq'<input type="text" size="25" name="title$id" value="$title">',
	  qq'<input type="text" size="25" name="uri$id" value="$uri">',
	  qq'<input type="text" size="50" name="desc$id" value="$description">',
	  qq'<input type="submit" name="del$id" value="Remove">',
	  ];

  }
  $oq->finish;
 
  push @table, [
      '',
      qq'<input type="text" size="25" name="title" value="$p->{title}">',
      qq'<input type="text" size="25" name="uri" value="$p->{uri}">',
      qq'<input type="text" size="50" name="desc" value="$p->{desc}">',
      qq'<input type="submit" name="add" value="Add">',
      ];

  my $table = table(@table);
  my $content = qq'
<p><a href="/hal/lokaler/eval">Besvar</a></p>
<form action="/hal/lokaler/admin" method="post">
$table
<br>
      <input type="submit" name="store" value="Store">

</form>';

  return outputFrontPage("lokaler", "Lokaler", $content);
}

sub evalPage { 
  my ($r,$q,$p) = @_;

  my %old;
  my $oa = db->sql("select space_option_id, points, comment from space_option_eval where member_id=?",
		   getSession->{member_id});
  while (my ($id, $points, $comment) = $oa->fetchrow_array) {
      $old{$id} = {
	  points=>$points,
	  comment=>$comment
      };
  }
  $oa->finish;

  
  my @table = [ qw'Navn Beskrivelse Kontingent Kommentar' ];
  my $oq = db->sql("select id, title, uri, description from space_option order by title");
  my $error;
  while (my ($id, $title, $uri, $description) = $oq->fetchrow_array) {
      my $old = $old{$id};

      my $fail = '';
      
      if ($p->{store}) {
	  if (defined $p->{"points$id"} && $p->{"points$id"} ne '') {
	      if ($p->{"points$id"} =~ /^\d+$/) {

		  if ($old) {
		      if ($p->{"points$id"} != $old->{points} or $p->{"comment$id"} ne $old->{comment}) {
			  db->sql("update space_option_eval set points=?, comment=? where member_id=? and space_option_id=?",
				  $p->{"points$id"}, $p->{"comment$id"}, getSession->{member_id}, $id);
		      }
		  } else {
		      db->sql("insert into space_option_eval (member_id,space_option_id, points, comment) values (?,?,?,?)",
			      getSession->{member_id}, $id, $p->{"points$id"}, $p->{"comment$id"});
		  }

	      } else {		  
		  $fail = "Dit kontingent-max skal være et hel-tal større end eller lig 0";
	      }
	  } else {
	      $fail = "Udfyld venligst dit kontingent-max for alle mulighederne";
	  }

	  $old = {
	      points=>$p->{"points$id"},
	      comment=>$p->{"comment$id"}
	  };
      }

      $old //= {
	  points=>'',
	  comment=>''
      };

      #die $fail. Dumper \%old;
      
      $error ||= $fail;
      my $class = $fail ? 'fail' : '';
      
      push @table, [
	  qq'<a target="lokale$id" href="$uri">$title</a>',
	  $description,
	  qq'<input class="$class" title="$fail" type="text" size="4" name="points$id" value="$old->{points}">',
	  qq'<input type="text" size="70" name="comment$id" value="$old->{comment}">',
	  ];

  }
  $oq->finish;

  return outputGoto('/hal/lokaler/result?fresh=1') if $p->{store} && !$error;      

  if ($error) {
      $error = "<p><strong>Svaret blev ikke gemt: </strong>$error</p>";
  }
  
  my $table = table(@table);

  my $poke = "";
  if ($p->{missing}) {
      if (%old) {
	  if ($p->{missing} == 1) {
	      $poke = "<p><strong>Hov!</strong> Der er kommet en ny valgmulighed, fortæl os hvad du synes om den, du kan se resultatet når du har svaret.</p>";
	  } else {
	      $poke = "<p><strong>Hov!</strong> Der er kommet $p->{missing} nye valgmuligheder, fortæl os hvad du synes om dem, du kan se resultatet når du har svaret.</p>";
	  }
      } else {
	  $poke = "<p><strong>Hov!</strong> Det ser ud til at du mangler at svare på undersøgelsen, du kan se resultatet når du har svaret.</p>";
      }
  }
  
  my $content = qq'$poke
<p>
Dette er <a target="masterplan" href="http://osaa.dk/wiki/index.php/Lokaler2020MasterPlan">de mest konkrete muligheder</a> der lige nu er for nye lokaler til OSAA, det kan ske at der i fremtiden kommer flere, det kan også ske at der bliver fjernet muligheder fra listen.
</p>
<p>
Flølg linket i Kolonnen med Navn for at se en beskrivelse af hver mulighed.
</p>
<p>
Du bedes nøje overveje hver mulighed og fortælle os hvor meget du vil betale i kontingent per måned for at være medlem, samt evt. en kommentar. Som rettesnor så er et kontingent på 215 kr det samme som vi startede med på KBV105.
Antag at alle andre vil være med til at betale det samme som dig, mao. skal du ikke tænke over hvad andre mener, kun hvad værdien er for dig.
</p>
<p>
Dette er <strong>ikke</strong> en afstemning. Den mest populære løsning bliver ikke valgt så snart der er nok "stemmer".
Besvarelserne er hverken bindende eller private, men vil blive brugt af bestyrelsen til at udforme forslag til generalforsamlingen.
Flytningen til nye lokaler vil være til afstemning på generalforsamlingen, som senest skal afholdes 1/6. Du kan gemme og rette din evaluering så tit du vil. Der er ingen deadline.
</p>
<form action="/hal/lokaler/eval" method="post">
$table
<br>
$error
      <input type="submit" name="store" value="Gem">

</form>';

  return outputFrontPage("lokaler", "Lokaler", $content);
}


BEGIN {
    ensureLogin(qr'^/hal/lokaler');
    ensureAdmin(qr'^/hal/lokaler/admin');
    addHandler(qr'^/hal/lokaler/admin$', \&adminPage);
    addHandler(qr'^/hal/lokaler/eval$', \&evalPage);
}

42;
