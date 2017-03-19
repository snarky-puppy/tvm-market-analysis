#!/usr/bin/perl -w

use strict;
use warnings;

use POSIX;

my $symbol = $ARGV[0];

die "Must give sym as 1st arg\n" unless length($symbol) > 0;

open my $fh, '<', "data/$symbol" or die $!;

my $si_txt;
my $ioh_txt;
my $ioh_shares_txt;
my $io_txt;
my $io_shares_txt;

while(<$fh>) {
    if(/^name : .*Short Interest/) {
        $si_txt = <$fh>;
    } elsif(/^name :.*Institutional Ownership History/) {
        $ioh_txt = <$fh>;
        my $tmp = <$fh>;
        while($tmp !~ /^name :.*Shares Outstanding/) { $tmp = <$fh> } # skip a few lines
        $ioh_shares_txt = <$fh>;

    } elsif(/^name :.*Insider Ownership/) {
        $io_txt = <$fh>;
        my $tmp = <$fh>;
        while($tmp !~ /^name :.*Shares Outstanding/) { $tmp = <$fh> } # skip a few lines
        $io_shares_txt = <$fh>;
    }
}

sub clean {
    my ($str) = (@_);
    if(!defined($str) or $str !~ /\[\[/) {
        $str = "";
    } else {
        $str =~ s/.*\[\[/[/;
        $str =~ s/\]\].*/]/;
    }
    return $str;
}

sub do_write {
    my ($symbol, $name, $headers, $data) = (@_);

    open my $fh, '>', 'results/'.$symbol.'_'.$name.'.csv' or die $!;
    print $fh $headers."\n";
    for(split /\],\[/, $data) {
        s/\[//g;
        s/\]//g;
        my @arr = split /,/;
        $arr[0] = POSIX::strftime("%d/%m/%Y", localtime($arr[0]/1000));
        print $fh join ",", @arr;
        print $fh "\n";
    }
    close $fh;
}

$si_txt = clean($si_txt);
$ioh_txt = clean($ioh_txt);
$ioh_shares_txt = clean($ioh_shares_txt);
$io_txt = clean($io_txt);
$io_shares_txt = clean($io_shares_txt);

#print "si_txt: ".$si_txt."\n";
#print "ioh_txt: ".$ioh_txt."\n";
#print "ioh_shares_txt: ".$ioh_shares_txt."\n";
#print "io_txt: ".$io_txt."\n";
#print "io_shares_txt: ".$io_shares_txt."\n";

do_write($symbol, 'short-interest-history', 'Date,Short Interest (%), Short Interest (Shares in Mil)', $si_txt) unless length($si_txt) == 0;
do_write($symbol, 'institutional-owners-history', 'Date,Institutional Ownership (%),Institutional Ownership (Shares in Mil)', $ioh_txt) unless length($ioh_txt) == 0;
do_write($symbol, 'inside-owners-history', 'Date,Insider Ownership (%),Insider Ownership (Shares in Mil)', $io_txt) unless length($io_txt) == 0;


