#-*-perl-*- $Id: Util.pm 3172 2006-12-22 19:58:04Z ff $
package HAL::Util;
require Exporter;
@ISA = qw(Exporter);
@EXPORT = qw(escape_url unescape_url encode_hidden randomdigits randomstring passwordHash passwordVerify passwordVerifyWithUpgrade);

use strict;
use warnings;
use HTML::Entities;
use Digest::SHA qw(sha256_hex);
use Crypt::Eksblowfish::Bcrypt qw(bcrypt_hash en_base64 de_base64);
use Time::HiRes qw(gettimeofday);

# TODO: Figure out how to upgrade to Crypt::Eksblowfish::Bcrypt
my $MIN_COST = 11;

# -----------------------------------------------------------------------
my %escapes;
for (0..255) {
    $escapes{chr($_)} = sprintf("%%%02X", $_);
}

# -----------------------------------------------------------------------
sub escape_url($) {
    my ($value) = @_;
    $value =~ s/([^a-zA-Z0-9])/$escapes{$1}/g;
    return $value;
}

sub unescape_url($) {
    my ($value) = @_;
    $value =~ s/\%([0-9A-Fa-f]{2})/chr(oct('0x'.$1))/ge;
    return $value;
}

sub encode_hidden($) {
    my ($f) = @_;
    return '' unless defined $f;
    my $o = '';
    while (my ($field,$value) = each %$f) {
	my $v = encode_entities($value);
	$o .= qq|<input type="hidden" name="$field" value="$v">\n|;
    }
    return $o;
}

sub randomstring($) {
  my ($length) = @_;
  my $data;

  local *FILE;
  open (FILE, "</dev/urandom") or die "Couldn't read from /dev/urandom";
  read(FILE, $data, $length) or die "Couldn't read from /dev/urandom";
  close FILE;
  return sprintf ("%02x" x $length, unpack ("C$length", $data));
}

sub randomdigits($) {
    my ($length) = @_;
    
    my $data;
    local *FILE;
    open (FILE, "</dev/urandom") or die "Couldn't read from /dev/urandom";
    read(FILE, $data, $length) or die "Couldn't read from /dev/urandom";
    close FILE;
    
    my $out;
    for my $i (0..$length-1) {
	$out .= int(ord(substr($data,$i,1))/25.6);
    }
    return $out;
}

sub passwordHash($) {
    my $passwd = shift;

    # TODO: Increase cost when CPUs are powerful enough to do this in less than 100ms
    my $cost = $MIN_COST; 
    my $salt = randomstring(8);
    my $hash = en_base64(bcrypt_hash({
	key_nul => 1,
	cost => $cost,
	salt => $salt,
    }, $passwd));
    
    return join ':', 'bcrypt', $salt, $cost, $hash;
}

sub passwordVerify($$) {
    my ($hash, $passwd) = @_;

    my ($scheme,$payload) = split /:/, $hash, 2;
    
    if ($scheme eq 'sha256') {
	my ($salt, $hash) = split /:/, $payload;

	return 0 unless sha256_hex("$salt:$passwd") eq $hash;

	return {
	    upgrade=>1,	    
	};
	
    } elsif ($scheme eq 'bcrypt') {
	my ($salt, $cost, $hash) = split /:/, $payload;

	my $ok = $hash eq en_base64(bcrypt_hash({
	    key_nul => 1,
	    cost => $cost,
	    salt => $salt,
        }, $passwd));	

	return 0 unless $ok;
	
	return {
	    upgrade=>$cost < $MIN_COST
	};
	
    } else {
	die "Unknown password scheme $scheme";
    }
}

sub passwordVerifyWithUpgrade {
    my ($hash, $passwd, $id, $db) = @_;

    my $ok = passwordVerify($hash, $passwd);
    return 0 unless $ok;
    if ($ok->{upgrade}) {
	my $upgradedHash = passwordHash($passwd);	
	$db->sql('update member set passwd=? where id=?',
		 $upgradedHash, $id);	
    }

    return $ok;
}

1;

